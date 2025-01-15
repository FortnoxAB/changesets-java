package se.fortnox.changesets;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;

@Singleton
public class VersionReader {
	private final Path versionFile;
	private final Log log;

	@Inject
	public VersionReader(MavenProject project, Log log) {
		this.versionFile = project.getBasedir().toPath().resolve(CHANGESET_DIR).resolve("VERSION");
		this.log = log;
	}

	String currentVersion() {
		try {
			log.info("Reading version from " + versionFile);
			return Files.readString(versionFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
