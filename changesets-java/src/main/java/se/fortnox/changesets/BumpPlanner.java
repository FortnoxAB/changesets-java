package se.fortnox.changesets;

import org.slf4j.Logger;
import se.fortnox.changesets.ChangesetsConfig.Bom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;
import static se.fortnox.changesets.ChangesetsConfig.VersioningStrategy.FIXED;

/**
 * Resolves which modules should be bumped to which versions for a release, given the
 * full set of changesets, the current reactor state, and the configured versioning strategy.
 * <p>
 * Pure function: no I/O, no side effects.
 */
public class BumpPlanner {
	private static final Logger LOG = getLogger(BumpPlanner.class);

	public record ModuleBump(String artifactId, String currentVersion, String newVersion, List<Changeset> changesets) {
		public boolean isVersionChange() {
			return !currentVersion.equals(newVersion);
		}
	}

	public static Map<String, ModuleBump> plan(
		List<Changeset> changesets,
		Map<String, String> reactor,
		ChangesetsConfig config
	) {
		Set<String> known = reactor.keySet();
		List<Changeset> known_changesets = filterKnownModules(changesets, known);

		List<Group> groups = buildGroups(reactor, config);

		Map<String, ModuleBump> result = new LinkedHashMap<>();
		for (Group group : groups) {
			List<Changeset> groupChangesets = changesetsForGroup(known_changesets, group);
			if (groupChangesets.isEmpty()) {
				continue;
			}
			result.putAll(planGroup(group, groupChangesets, reactor));
		}

		if (config.bom() != null) {
			applyBomPlan(result, known_changesets, reactor, config.bom());
		}

		return result;
	}

	/**
	 * Applies BOM (Bill of Materials) semantics on top of the base plan:
	 * <ul>
	 *   <li>Removes the consumer-parent from the plan (it inherits its version from the BOM).</li>
	 *   <li>Synthesizes/merges a BOM bump at the max level of any tracked module bump,
	 *       combined with any explicit BOM-targeted changesets.</li>
	 *   <li>The BOM bump's {@code changesets} list contains only the explicit changesets
	 *       targeting the BOM, so changelog rendering shows them naturally; the synthesized
	 *       part is purely a version-bump signal.</li>
	 * </ul>
	 */
	private static void applyBomPlan(
		Map<String, ModuleBump> result,
		List<Changeset> allChangesets,
		Map<String, String> reactor,
		Bom bom
	) {
		String bomModule = bom.module();
		String consumerParent = bom.consumerParent();

		if (consumerParent != null) {
			result.remove(consumerParent);
		}

		EnumSet<Level> trackedLevels = EnumSet.noneOf(Level.class);
		for (ModuleBump bump : result.values()) {
			if (bump.artifactId().equals(bomModule)) {
				continue;
			}
			if (!bump.isVersionChange()) {
				continue;
			}
			for (Changeset c : bump.changesets()) {
				trackedLevels.add(c.level());
			}
		}

		List<Changeset> bomExplicit = allChangesets.stream()
			.filter(c -> c.packageName().equals(bomModule))
			.toList();

		if (trackedLevels.isEmpty() && bomExplicit.isEmpty()) {
			return;
		}

		List<Changeset> combinedForVersionCalc = new ArrayList<>(bomExplicit);
		for (Level level : trackedLevels) {
			combinedForVersionCalc.add(new Changeset(bomModule, level, "", null));
		}

		String bomCurrent = reactor.get(bomModule);
		if (bomCurrent == null) {
			throw new IllegalArgumentException(
				"bom.module '" + bomModule + "' is not present in the reactor");
		}
		String bomNew = VersionCalculator.getNewVersion(bomCurrent, combinedForVersionCalc);

		result.put(bomModule, new ModuleBump(bomModule, bomCurrent, bomNew, bomExplicit));
	}

	private static List<Changeset> filterKnownModules(List<Changeset> changesets, Set<String> known) {
		List<Changeset> kept = new ArrayList<>(changesets.size());
		for (Changeset c : changesets) {
			if (known.contains(c.packageName())) {
				kept.add(c);
			} else {
				LOG.warn("Changeset {} references unknown module '{}', ignoring",
					c.file() == null ? "<unknown>" : c.file().getName(),
					c.packageName());
			}
		}
		return kept;
	}

	private static List<Group> buildGroups(Map<String, String> reactor, ChangesetsConfig config) {
		if (config.versioning() == FIXED) {
			return List.of(new Group(GroupKind.FIXED, new LinkedHashSet<>(reactor.keySet())));
		}

		List<Group> groups = new ArrayList<>();
		Set<String> assigned = new HashSet<>();
		for (List<String> g : config.fixed()) {
			groups.add(new Group(GroupKind.FIXED, new LinkedHashSet<>(g)));
			assigned.addAll(g);
		}
		for (List<String> g : config.linked()) {
			groups.add(new Group(GroupKind.LINKED, new LinkedHashSet<>(g)));
			assigned.addAll(g);
		}
		for (String artifactId : reactor.keySet()) {
			if (!assigned.contains(artifactId)) {
				groups.add(new Group(GroupKind.INDIVIDUAL, new LinkedHashSet<>(List.of(artifactId))));
			}
		}
		return groups;
	}

	private static List<Changeset> changesetsForGroup(List<Changeset> changesets, Group group) {
		return changesets.stream()
			.filter(c -> group.members().contains(c.packageName()))
			.toList();
	}

	private static Map<String, ModuleBump> planGroup(Group group, List<Changeset> groupChangesets, Map<String, String> reactor) {
		String baseVersion = highestVersion(group.members().stream()
			.map(reactor::get)
			.filter(java.util.Objects::nonNull)
			.toList());
		String newVersion = VersionCalculator.getNewVersion(baseVersion, groupChangesets);

		Set<String> activeMembers = groupChangesets.stream()
			.map(Changeset::packageName)
			.collect(Collectors.toCollection(LinkedHashSet::new));

		// Iterate in declaration order so changelog output is stable regardless of changeset file order
		LinkedHashSet<String> bumpMembers = new LinkedHashSet<>();
		for (String member : group.members()) {
			boolean include = switch (group.kind()) {
				case FIXED, INDIVIDUAL -> true;
				case LINKED -> activeMembers.contains(member);
			};
			if (include) {
				bumpMembers.add(member);
			}
		}

		Map<String, ModuleBump> result = new LinkedHashMap<>();
		for (String member : bumpMembers) {
			String currentVersion = reactor.get(member);
			if (currentVersion == null) {
				continue;
			}
			List<Changeset> memberChangesets = groupChangesets.stream()
				.filter(c -> c.packageName().equals(member))
				.toList();
			result.put(member, new ModuleBump(member, currentVersion, newVersion, memberChangesets));
		}
		return result;
	}

	private static String highestVersion(Collection<String> versions) {
		TreeMap<org.semver4j.Semver, String> sorted = new TreeMap<>();
		for (String v : versions) {
			org.semver4j.Semver semver = Optional.ofNullable(org.semver4j.Semver.coerce(v))
				.map(org.semver4j.Semver::withClearedPreReleaseAndBuild)
				.orElseThrow(() -> new IllegalArgumentException("Cannot coerce \"%s\" into a semantic version.".formatted(v)));
			sorted.put(semver, v);
		}
		if (sorted.isEmpty()) {
			throw new IllegalStateException("Group has no resolvable members in reactor");
		}
		return sorted.lastEntry().getValue();
	}

	private enum GroupKind { FIXED, LINKED, INDIVIDUAL }

	private record Group(GroupKind kind, Set<String> members) {}
}
