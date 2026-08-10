package se.fortnox.changesets.maven.policy;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.changesets.VersionsFile;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;
import static se.fortnox.changesets.VersionCalculator.nextDevelopmentVersion;

/**
 * VersionPolicy for the maven-release-plugin that resolves each module's release/next-dev version
 * from {@code .changeset/VERSIONS} (written by {@code changesets:prepare}).
 * <p>
 * The maven-release-plugin's {@code VersionPolicyRequest} only carries {@code version} and the
 * reactor {@code workingDirectory} — no artifactId — so this policy identifies the calling module
 * by matching {@code request.getVersion()} first against reactor pom versions (map-release-versions
 * phase) and then, if that fails, against VERSIONS values (map-development-versions phase, where
 * release-plugin passes the just-computed release version). If exactly one candidate is found, its
 * artifactId keys into VERSIONS. On ambiguous matches, the policy accepts the ambiguity if all
 * candidates share the same target; otherwise it leaves the version unchanged with an actionable
 * warning.
 */
@Component(role = VersionPolicy.class,
	hint = "changesets",
	description = "A VersionPolicy implementation that uses changesets-java to calculate the current and next version per Maven module.")
public class ChangesetsVersionPolicy implements VersionPolicy {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final MavenProject project;
	private final MavenSession session;

	@Inject
	public ChangesetsVersionPolicy(MavenProject project, MavenSession session) {
		this.project = project;
		this.session = session;
	}

	@Override
	public VersionPolicyResult getReleaseVersion(VersionPolicyRequest request) throws PolicyException, VersionParseException {
		return new VersionPolicyResult().setVersion(
			lookup(request)
				.map(ChangesetsVersionPolicy::stripSnapshot)
				.orElseGet(() -> stripSnapshot(request.getVersion())));
	}

	@Override
	public VersionPolicyResult getDevelopmentVersion(VersionPolicyRequest request) throws PolicyException, VersionParseException {
		return new VersionPolicyResult().setVersion(
			lookup(request)
				.map(v -> nextDevelopmentVersion(stripSnapshot(v)))
				// Unmapped: return a SNAPSHOT so release-plugin's dev-version loop terminates.
				// If the request is already a SNAPSHOT (typical: unmapped module retaining its
				// pre-release version), leave it as-is. If it's not (typical: release-plugin's
				// dev-version phase passing us the just-computed release version for a module
				// we can no longer identify), bump to next-dev.
				.orElseGet(() -> ensureSnapshot(request.getVersion())));
	}

	private static String ensureSnapshot(String version) {
		if (version == null) {
			return nextDevelopmentVersion("0.0.0");
		}
		return version.endsWith("-SNAPSHOT") ? version : nextDevelopmentVersion(version);
	}

	private Optional<String> lookup(VersionPolicyRequest request) {
		Optional<Path> reactorRoot = findReactorRoot(reactorRootHint(request));
		if (reactorRoot.isEmpty()) {
			logger.info("No .changeset dir found; using current version {}", request.getVersion());
			return Optional.empty();
		}
		Map<String, String> versions = VersionsFile.read(reactorRoot.get());
		List<String> candidates = candidateArtifactIds(request, versions);
		if (candidates.isEmpty()) {
			logger.info("No VERSIONS match for version {}; leaving version unchanged", request.getVersion());
			return Optional.empty();
		}
		if (candidates.size() == 1) {
			String mapped = versions.get(candidates.get(0));
			logger.info("Resolved release version for {} from VERSIONS: {}", candidates.get(0), mapped);
			return Optional.of(mapped);
		}
		// Ambiguous: multiple reactor modules share the same current version. If they all map to
		// the same target in VERSIONS (typical for BOM / fixed / linked groups that bump together),
		// that target is unambiguous even if the artifactId is not.
		Set<String> distinctTargets = candidates.stream().map(versions::get).collect(Collectors.toSet());
		if (distinctTargets.size() == 1) {
			String mapped = distinctTargets.iterator().next();
			logger.info("Ambiguous artifact for version {} but all candidates map to {}; using it. Candidates: {}",
				request.getVersion(), mapped, candidates);
			return Optional.of(mapped);
		}
		logger.warn(
			"Ambiguous VERSIONS lookup for version {}: candidates {} map to differing targets {}; " +
				"leaving version unchanged. Combining maven-release-plugin with independent versioning " +
				"is not supported — use `changesets:prepare` + `changesets:release` instead.",
			request.getVersion(), candidates, distinctTargets);
		return Optional.empty();
	}

	/**
	 * Reactor artifactIds present in VERSIONS that could correspond to the requested version.
	 * <p>
	 * Tries two matches, in order of specificity:
	 * <ol>
	 *   <li>Reactor project's current pom version equals the requested version — the normal case
	 *       during the map-release-versions phase.</li>
	 *   <li>VERSIONS value equals the requested version — needed for the map-development-versions
	 *       phase, where release-plugin passes us the just-computed release version (which is the
	 *       VERSIONS value from the prior phase) rather than the module's current pom version.</li>
	 * </ol>
	 */
	private List<String> candidateArtifactIds(VersionPolicyRequest request, Map<String, String> versions) {
		if (session == null || session.getProjects() == null) {
			return List.of();
		}
		String requested = request.getVersion();
		if (requested == null) {
			return List.of();
		}
		Map<String, MavenProject> byArtifactId = new LinkedHashMap<>();
		for (MavenProject p : session.getProjects()) {
			byArtifactId.put(p.getArtifactId(), p);
		}
		List<String> byCurrentPomVersion = versions.keySet().stream()
			.filter(byArtifactId::containsKey)
			.filter(a -> requested.equals(byArtifactId.get(a).getVersion()))
			.toList();
		if (!byCurrentPomVersion.isEmpty()) {
			return byCurrentPomVersion;
		}
		return versions.entrySet().stream()
			.filter(e -> requested.equals(e.getValue()))
			.map(Map.Entry::getKey)
			.toList();
	}

	private Path reactorRootHint(VersionPolicyRequest request) {
		String wd = request.getWorkingDirectory();
		if (wd != null && !wd.isBlank()) {
			return Path.of(wd);
		}
		return project.getBasedir().toPath();
	}

	private static Optional<Path> findReactorRoot(Path start) {
		Path current = start.toAbsolutePath();
		while (current != null) {
			if (Files.isDirectory(current.resolve(CHANGESET_DIR))) {
				return Optional.of(current);
			}
			current = current.getParent();
		}
		return Optional.empty();
	}

	private static String stripSnapshot(String version) {
		if (version == null) {
			return "0.0.0";
		}
		return version.endsWith("-SNAPSHOT")
			? version.substring(0, version.length() - "-SNAPSHOT".length())
			: version;
	}
}
