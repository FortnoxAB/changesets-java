package se.fortnox.changesets;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;

@Singleton
public class VersionFile {
	private final Path versionFile;

	@Inject
	public VersionFile(MavenProject project, Logger log) {
		this.versionFile = project.getBasedir().toPath().resolve(CHANGESET_DIR).resolve("VERSION").toAbsolutePath();
		log.info("Using version file " + versionFile);
	}

	public Optional<String> currentVersion() {
		if(!Files.exists(versionFile)) {
			return Optional.empty();
		}
		try {
			return Optional.of(Files.readString(versionFile));
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	public void assignVersion(String newVersion) {
		try {
			Files.writeString(versionFile, newVersion, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
