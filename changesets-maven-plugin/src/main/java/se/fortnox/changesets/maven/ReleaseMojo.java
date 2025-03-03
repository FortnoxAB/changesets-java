package se.fortnox.changesets.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import se.fortnox.changesets.VersionFile;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Mojo(name = "release", defaultPhase = LifecyclePhase.INITIALIZE, aggregator = true)
public class ReleaseMojo extends AbstractMojo {
	private final MavenProject project;
	private final VersionFile versionFile;
	private final Logger log;

	@Inject
	public ReleaseMojo(MavenProject project, VersionFile versionFile, Logger log) {
		this.project = project;
		this.versionFile = versionFile;
		this.log = log;
	}

	public void execute() {
		Path baseDir = project.getBasedir().toPath();
		String pomVersion = versionFile.currentVersion().orElse("0.0.0");

		log.info("Updating " + project.getFile() + " to " + pomVersion);
		PomUpdater.setProjectVersion(project.getFile(), pomVersion);

		// Update submodules to reference the parent project with the new version
		List<String> modules = project.getModules();
		modules.forEach(module -> {
			File modulePom = baseDir.resolve(module).resolve("pom.xml").toFile();
			log.info("Updating submodule" + modulePom + " to " + pomVersion);
			PomUpdater.setProjectParentVersion(modulePom, pomVersion);
		});
	}
}
