package se.fortnox.changesets.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.semver4j.Semver;
import se.fortnox.changesets.ChangelogAggregator;
import se.fortnox.changesets.Changeset;
import se.fortnox.changesets.ChangesetLocator;
import se.fortnox.changesets.VersionCalculator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;

/**
 * Applies all changesets into the changelog and calculates the new version number.
 * <p>
 * This would normally be the last step in preparing a release PR.
 */
@Mojo(name = "prepare", defaultPhase = LifecyclePhase.INITIALIZE)
public class PrepareMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private org.apache.maven.project.MavenProject project;

	public void execute() {
		Path baseDir = project.getBasedir().toPath();
		String packageName = project.getArtifactId();

		// Get all relevant changesets
		ChangesetLocator changesetLocator = new ChangesetLocator(baseDir);
		List<Changeset> changesets = changesetLocator.getChangesets(packageName);

		if (changesets.isEmpty()) {
			getLog().info("No changesets for package: " + packageName + " found in " + baseDir);
			return;
		}

		// Calculate new version
		String currentVersion = getCurrentVersion();
		String newVersion = VersionCalculator.getNewVersion(currentVersion, changesets);
		getLog().info("Old version was " + currentVersion + ", will be updated to " + newVersion);

		// Move changesets into CHANGELOG.md
		ChangelogAggregator changelogAggregator = new ChangelogAggregator(baseDir);
		changelogAggregator.mergeChangesetsToChangelog(packageName, newVersion);

		try {
			Files.writeString(project.getBasedir().toPath().resolve(CHANGESET_DIR).resolve("VERSION"), newVersion, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String getCurrentVersion() {
		try {
			Path versionFile = project.getBasedir().toPath().resolve(CHANGESET_DIR).resolve("VERSION");
			getLog().info("Reading version from " + versionFile);
			return Files.readString(versionFile);

		} catch (NoSuchFileException exception) {
			return "0.0.0";

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
