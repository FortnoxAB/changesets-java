package se.fortnox.changesets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public record ChangesetsConfig(
	VersioningStrategy versioning,
	List<List<String>> linked,
	List<List<String>> fixed,
	ChangelogMode changelog,
	Bom bom
) {
	private static final Logger LOG = getLogger(ChangesetsConfig.class);
	public static final String CONFIG_FILE = "config.json";

	public ChangesetsConfig {
		versioning = versioning == null ? VersioningStrategy.FIXED : versioning;
		linked = linked == null ? List.of() : List.copyOf(linked);
		fixed = fixed == null ? List.of() : List.copyOf(fixed);
		changelog = changelog == null ? ChangelogMode.ROOT : changelog;
		validateGroupsAreDisjoint(linked, fixed);
		validateChangelogModeAgainstVersioning(versioning, changelog);
	}

	public static ChangesetsConfig defaults() {
		return new ChangesetsConfig(VersioningStrategy.FIXED, List.of(), List.of(), ChangelogMode.ROOT, null);
	}

	/**
	 * Read .changeset/config.json from the given changesets directory.
	 * Returns defaults if the file does not exist or cannot be parsed.
	 */
	public static ChangesetsConfig load(Path changesetsDir) {
		Path configFile = changesetsDir.resolve(CONFIG_FILE);
		if (!Files.exists(configFile)) {
			return defaults();
		}
		try {
			String json = Files.readString(configFile);
			return new ObjectMapper().readValue(json, ChangesetsConfig.class);
		} catch (IOException e) {
			LOG.error("Failed to read changesets config at {}, falling back to defaults", configFile, e);
			return defaults();
		}
	}

	private static void validateChangelogModeAgainstVersioning(VersioningStrategy versioning, ChangelogMode changelog) {
		if (changelog == ChangelogMode.MODULE && versioning == VersioningStrategy.FIXED) {
			throw new IllegalArgumentException(
				"changelog mode 'module' requires versioning 'independent'; "
					+ "with 'fixed' versioning all modules share a single version, so per-module changelogs are not meaningful");
		}
	}

	private static void validateGroupsAreDisjoint(List<List<String>> linked, List<List<String>> fixed) {
		Set<String> seen = new HashSet<>();
		List<List<String>> allGroups = new ArrayList<>(linked.size() + fixed.size());
		allGroups.addAll(linked);
		allGroups.addAll(fixed);
		for (List<String> group : allGroups) {
			for (String name : group) {
				if (!seen.add(name)) {
					throw new IllegalArgumentException(
						"Module '" + name + "' appears in multiple linked/fixed groups");
				}
			}
		}
	}

	public enum VersioningStrategy {
		@JsonProperty("fixed") FIXED,
		@JsonProperty("independent") INDEPENDENT;

		@JsonCreator
		public static VersioningStrategy fromString(String value) {
			if (value == null) {
				return FIXED;
			}
			return switch (value.toLowerCase()) {
				case "fixed" -> FIXED;
				case "independent" -> INDEPENDENT;
				default -> throw new IllegalArgumentException("Unknown versioning strategy: " + value);
			};
		}
	}

	/**
	 * Where release changelog entries are written.
	 *
	 * <ul>
	 *   <li>{@code ROOT} (default): a single aggregated {@code CHANGELOG.md} at the reactor root.</li>
	 *   <li>{@code MODULE}: each bumped module gets its own {@code CHANGELOG.md} next to its
	 *       {@code pom.xml}. When a BOM is configured, the reactor-root {@code CHANGELOG.md} is
	 *       additionally written as a rollup summarising the per-module bumps for that BOM version.
	 *       Requires {@code versioning: independent}.</li>
	 * </ul>
	 */
	public enum ChangelogMode {
		@JsonProperty("root") ROOT,
		@JsonProperty("module") MODULE;

		@JsonCreator
		public static ChangelogMode fromString(String value) {
			if (value == null) {
				return ROOT;
			}
			return switch (value.toLowerCase()) {
				case "root" -> ROOT;
				case "module" -> MODULE;
				default -> throw new IllegalArgumentException("Unknown changelog mode: " + value);
			};
		}
	}

	/**
	 * Optional BOM (Bill of Materials) configuration. When set, the BOM module's
	 * {@code <properties>} that pin sibling module versions are rewritten on every
	 * prepare, and the BOM itself is auto-bumped at the max level of any tracked
	 * module's bump. An optional {@code consumerParent} provides the changelog header
	 * artifactId; it inherits its version from the BOM via Maven parent inheritance.
	 */
	public record Bom(String module, String consumerParent) {
		public Bom {
			if (module == null || module.isBlank()) {
				throw new IllegalArgumentException("bom.module must be set");
			}
			if (consumerParent != null && consumerParent.isBlank()) {
				consumerParent = null;
			}
		}
	}
}
