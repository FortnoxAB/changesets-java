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

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static se.fortnox.changesets.VersionCalculator.nextDevelopmentVersion;

@Component(role = VersionPolicy.class,
	hint = "changesets",
	description = "A VersionPolicy implementation that uses changesets-java to calculate the current and next version.")
public class ChangesetsVersionPolicy implements VersionPolicy {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	public static final String CHANGESET_DIR = ".changeset";

	private final MavenProject project;

	@Inject
	public ChangesetsVersionPolicy(MavenProject project) {
		this.project = project;
	}

	@Override
	public VersionPolicyResult getReleaseVersion(VersionPolicyRequest versionPolicyRequest) throws PolicyException, VersionParseException {
		Path versionFile = project.getBasedir().toPath().resolve(CHANGESET_DIR).resolve("VERSION");
		logger.info("Reading version from " + versionFile);
		try {
			String version = Files.readString(versionFile);
			return new VersionPolicyResult()
				.setVersion(version);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


	}

	@Override
	public VersionPolicyResult getDevelopmentVersion(VersionPolicyRequest versionPolicyRequest) throws PolicyException, VersionParseException {
		Path versionFile = project.getBasedir().toPath().resolve(CHANGESET_DIR).resolve("VERSION");
		try {
			String version = Files.readString(versionFile);
			return new VersionPolicyResult()
				.setVersion(nextDevelopmentVersion(version));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
