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

@Mojo(name = "release", defaultPhase = LifecyclePhase.INITIALIZE, aggregator = true)
public class ReleaseMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private org.apache.maven.project.MavenProject project;

	public void execute() {
		Path baseDir = project.getBasedir().toPath();
		String packageName = project.getArtifactId();

		String pomVersion = getCurrentVersion();


			getLog().info("Updating " + project.getFile() + " to " + pomVersion);
			PomUpdater.setProjectVersion(project.getFile(), pomVersion);

			// Update submodules to reference the parent project with the new version
			// TODO Do we need to check if the submodule has set it's version, so that also needs to be bumped?
			List<String> modules = project.getModules();
			modules.forEach(module -> {
				File modulePom = baseDir.resolve(module).resolve("pom.xml").toFile();
				getLog().info("Updating submodule" + modulePom + " to " + pomVersion);
				PomUpdater.setProjectParentVersion(modulePom, pomVersion);
			});
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
