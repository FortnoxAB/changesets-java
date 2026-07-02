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

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;
import static se.fortnox.changesets.VersionCalculator.nextDevelopmentVersion;

/**
 * VersionPolicy for the maven-release-plugin that resolves each module's release/next-dev version
 * from {@code .changeset/VERSIONS} (written by {@code changesets:prepare}).
 * <p>
 * The maven-release-plugin's {@code VersionPolicyRequest} only carries {@code version} and the
 * reactor {@code workingDirectory} — no artifactId — so this policy identifies the calling
 * module by matching {@code request.getVersion()} against the reactor projects' current versions.
 * If exactly one reactor project has that version, its artifactId is used as the VERSIONS key.
 * Otherwise (no match, or ambiguous match) the policy falls back to the request's version.
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
				// modules not tracked in VERSIONS are left unchanged
				.orElseGet(request::getVersion));
	}

	private Optional<String> lookup(VersionPolicyRequest request) {
		Optional<Path> reactorRoot = findReactorRoot(reactorRootHint(request));
		if (reactorRoot.isEmpty()) {
			logger.info("No .changeset dir found; using current version {}", request.getVersion());
			return Optional.empty();
		}
		Map<String, String> versions = VersionsFile.read(reactorRoot.get());
		Optional<String> artifactId = identifyModule(request, versions);
		if (artifactId.isEmpty()) {
			logger.info("No VERSIONS match for version {}; leaving version unchanged", request.getVersion());
			return Optional.empty();
		}
		String mapped = versions.get(artifactId.get());
		logger.info("Resolved release version for {} from VERSIONS: {}", artifactId.get(), mapped);
		return Optional.of(mapped);
	}

	/**
	 * Identify which reactor module the request is for by matching {@code request.getVersion()}
	 * against reactor projects' current versions, restricted to modules present in VERSIONS.
	 * Returns empty when no unique match exists.
	 */
	private Optional<String> identifyModule(VersionPolicyRequest request, Map<String, String> versions) {
		if (session == null || session.getProjects() == null) {
			return Optional.empty();
		}
		String requested = request.getVersion();
		Map<String, MavenProject> byArtifactId = new LinkedHashMap<>();
		for (MavenProject p : session.getProjects()) {
			byArtifactId.put(p.getArtifactId(), p);
		}
		List<String> candidates = versions.keySet().stream()
			.filter(byArtifactId::containsKey)
			.filter(a -> requested != null && requested.equals(byArtifactId.get(a).getVersion()))
			.toList();
		if (candidates.size() == 1) {
			return Optional.of(candidates.get(0));
		}
		if (candidates.size() > 1) {
			logger.warn("Ambiguous VERSIONS lookup for version {}: matches {}", requested, candidates);
		}
		return Optional.empty();
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
