package se.fortnox.changesets.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;

@Mojo(name = "release", defaultPhase = LifecyclePhase.INITIALIZE, aggregator = true)
public class ReleaseMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	public void execute() {
		Path baseDir = project.getBasedir().toPath();
		String pomVersion = getCurrentVersion();

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
