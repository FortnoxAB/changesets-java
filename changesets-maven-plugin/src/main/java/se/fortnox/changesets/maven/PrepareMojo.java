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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

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

		// Set newVersion property to be used by versions:set
		if (!newVersion.equals(currentVersion)) {
			String pomVersion = Optional.ofNullable(Semver.coerce(newVersion))
				.map(semver -> semver.withIncPatch().withPreRelease("SNAPSHOT").getVersion())
				.orElseThrow(() -> new IllegalArgumentException("Cannot coerce \"%s\" into a semantic version.".formatted(currentVersion)));


			getLog().info("Updating " + project.getFile() + " to " + pomVersion);
			PomUpdater.setProjectVersion(project.getFile(), pomVersion);

			// Update submodules to reference the parent project with the new version
			List<String> modules = project.getModules();
			modules.forEach(module -> {
				File modulePom = baseDir.resolve(module).resolve("pom.xml").toFile();
				getLog().info("Updating submodule" + modulePom + " to " + pomVersion);
				PomUpdater.setProjectParentVersion(modulePom, pomVersion);
			});
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
