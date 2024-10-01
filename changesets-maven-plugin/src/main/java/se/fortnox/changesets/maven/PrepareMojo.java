package se.fortnox.changesets.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.fortnox.changesets.ChangelogAggregator;
import se.fortnox.changesets.Changeset;
import se.fortnox.changesets.ChangesetLocator;
import se.fortnox.changesets.VersionCalculator;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

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
		Path   baseDir     = project.getBasedir().toPath();
		String packageName = project.getArtifactId();

		// Get all relevant changesets
		ChangesetLocator changesetLocator = new ChangesetLocator(baseDir);
		List<Changeset>   changesets        = changesetLocator.getChangesets(packageName);

		if (changesets.isEmpty()) {
			getLog().info("No changesets for package: " + packageName + " found in " + baseDir);
			return;
		}

		// Calculate new version
		String newVersion = VersionCalculator.getNewVersion(project.getVersion(), changesets);
		getLog().info("Old version was " + project.getVersion() + ", will be updated to " + newVersion);

		// Move changesets into CHANGELOG.md
		ChangelogAggregator changelogAggregator = new ChangelogAggregator(baseDir);
		changelogAggregator.mergeChangesetsToChangelog(packageName, newVersion);

		// Set newVersion property to be used by versions:set
		if (!newVersion.equals(project.getVersion())) {
			project.getProperties().setProperty("newVersion", newVersion);

			getLog().info("Updating " + project.getFile() + " to " + newVersion);
			PomUpdater.setProjectVersion(project.getFile(), newVersion);

			// Update submodules to reference the parent project with the new version
			// TODO Do we need to check if the submodule has set it's version, so that also needs to be bumped?
			List<String> modules = project.getModules();
			modules.forEach(module -> {
				File modulePom = baseDir.resolve(module).resolve("pom.xml").toFile();
				getLog().info("Updating submodule" + modulePom + " to " + newVersion);
				PomUpdater.setProjectParentVersion(modulePom, newVersion);
			});
		}
	}
}
