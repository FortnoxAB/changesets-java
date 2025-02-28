package se.fortnox.changesets.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.semver4j.Semver;
import se.fortnox.changesets.ChangelogAggregator;
import se.fortnox.changesets.Changeset;
import se.fortnox.changesets.ChangesetLocator;
import se.fortnox.changesets.VersionCalculator;
import se.fortnox.changesets.VersionFile;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Applies all changesets into the changelog and calculates the new version number.
 * <p>
 * This would normally be the last step in preparing a release PR.
 */
@Mojo(name = "prepare", defaultPhase = LifecyclePhase.INITIALIZE)
public class PrepareMojo extends AbstractMojo {
	private final org.apache.maven.project.MavenProject project;
	private final VersionFile versionFile;
	private final Logger logger;

	@Inject
	public PrepareMojo(MavenProject project, VersionFile versionFile, Logger logger) {
		this.project = project;
		this.versionFile = versionFile;
		this.logger = logger;
	}

	/*
	 * Set to true in order to just process changeset files, avoiding any changes to the POM(s).
	 */
	@Parameter(property = "useReleasePluginIntegration", defaultValue = "false")
	protected boolean useReleasePluginIntegration = false;

	public void execute() {
		Path baseDir = project.getBasedir().toPath();
		String packageName = project.getArtifactId();

		// Get all relevant changesets
		ChangesetLocator changesetLocator = new ChangesetLocator(baseDir);
		List<Changeset> changesets = changesetLocator.getChangesets(packageName);

		if (changesets.isEmpty()) {
			logger.info("No changesets for package: " + packageName + " found in " + baseDir);
			return;
		}

		// Calculate new version
		String currentVersion = versionFile.currentVersion().orElse("0.0.0");;
		String newVersion = VersionCalculator.getNewVersion(currentVersion, changesets);
		logger.info("Old version was " + currentVersion + ", will be updated to " + newVersion);

		// Move changesets into CHANGELOG.md
		ChangelogAggregator changelogAggregator = new ChangelogAggregator(baseDir);
		changelogAggregator.mergeChangesetsToChangelog(packageName, newVersion);

		// Advance to version deduced from changesets
		versionFile.assignVersion(newVersion);

		if(useReleasePluginIntegration) {
			logger.info("Changesets processed, but not updating POMs due to useReleasePluginIntegration being set to true.");
			return;
		}

		// Set newVersion property to be used by versions:set
		if (!newVersion.equals(currentVersion)) {
			String pomVersion = Optional.ofNullable(Semver.coerce(newVersion))
				.map(semver -> semver.withIncPatch().withPreRelease("SNAPSHOT").getVersion())
				.orElseThrow(() -> new IllegalArgumentException("Cannot coerce \"%s\" into a semantic version.".formatted(currentVersion)));


			logger.info("Updating " + project.getFile() + " to " + pomVersion);
			PomUpdater.setProjectVersion(project.getFile(), pomVersion);

			// Update submodules to reference the parent project with the new version
			List<String> modules = project.getModules();
			modules.forEach(module -> {
				File modulePom = baseDir.resolve(module).resolve("pom.xml").toFile();
				logger.info("Updating submodule" + modulePom + " to " + pomVersion);
				PomUpdater.setProjectParentVersion(modulePom, pomVersion);
			});
		}
	}
}
