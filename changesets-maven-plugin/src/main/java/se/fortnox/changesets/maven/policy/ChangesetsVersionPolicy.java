package se.fortnox.changesets.maven.policy;

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
import java.util.Optional;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;
import static se.fortnox.changesets.VersionCalculator.nextDevelopmentVersion;

@Component(role = VersionPolicy.class,
	hint = "changesets",
	description = "A VersionPolicy implementation that uses changesets-java to calculate the current and next version per Maven module.")
public class ChangesetsVersionPolicy implements VersionPolicy {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final MavenProject project;

	@Inject
	public ChangesetsVersionPolicy(MavenProject project) {
		this.project = project;
	}

	@Override
	public VersionPolicyResult getReleaseVersion(VersionPolicyRequest request) throws PolicyException, VersionParseException {
		String version = lookupOrCurrent(request);
		return new VersionPolicyResult().setVersion(stripSnapshot(version));
	}

	@Override
	public VersionPolicyResult getDevelopmentVersion(VersionPolicyRequest request) throws PolicyException, VersionParseException {
		String version = lookupOrCurrent(request);
		return new VersionPolicyResult().setVersion(nextDevelopmentVersion(stripSnapshot(version)));
	}

	private String lookupOrCurrent(VersionPolicyRequest request) {
		Optional<Path> reactorRoot = findReactorRoot(project.getBasedir().toPath());
		if (reactorRoot.isPresent()) {
			Optional<String> mapped = VersionsFile.lookup(reactorRoot.get(), project.getArtifactId());
			if (mapped.isPresent()) {
				logger.info("Resolved release version for {} from VERSIONS: {}", project.getArtifactId(), mapped.get());
				return mapped.get();
			}
		}
		logger.info("No VERSIONS entry for {}; using current version {}", project.getArtifactId(), request.getVersion());
		return request.getVersion();
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
