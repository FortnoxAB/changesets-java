package se.fortnox.changesets.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import se.fortnox.changesets.VersionsFile;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Mojo(name = "release", defaultPhase = LifecyclePhase.INITIALIZE, aggregator = true)
public class ReleaseMojo extends AbstractMojo {
	private final MavenProject project;
	private final MavenSession session;
	private final Logger log;

	@Inject
	public ReleaseMojo(MavenProject project, MavenSession session, Logger log) {
		this.project = project;
		this.session = session;
		this.log = log;
	}

	public void execute() {
		Path reactorRoot = project.getBasedir().toPath();
		Map<String, String> versions = VersionsFile.read(reactorRoot);
		if (versions.isEmpty()) {
			log.info("No release versions found in " + VersionsFile.locate(reactorRoot));
			return;
		}

		Map<String, MavenProject> byArtifactId = new LinkedHashMap<>();
		for (MavenProject p : session.getProjects()) {
			byArtifactId.put(p.getArtifactId(), p);
		}

		String rootArtifactId = project.getArtifactId();
		String rootReleaseVersion = null;

		for (Map.Entry<String, String> entry : versions.entrySet()) {
			MavenProject moduleProject = byArtifactId.get(entry.getKey());
			if (moduleProject == null) {
				log.warn("VERSIONS entry for unknown module: " + entry.getKey());
				continue;
			}
			File pomFile = moduleProject.getFile();
			log.info("Updating " + pomFile + " to " + entry.getValue());
			PomUpdater.setProjectVersion(pomFile, entry.getValue());

			if (entry.getKey().equals(rootArtifactId)) {
				rootReleaseVersion = entry.getValue();
			}
		}

		if (rootReleaseVersion != null) {
			Path baseDir = project.getBasedir().toPath();
			for (String module : project.getModules()) {
				File modulePom = baseDir.resolve(module).resolve("pom.xml").toFile();
				log.info("Updating submodule " + modulePom + " parent ref to " + rootReleaseVersion);
				PomUpdater.setProjectParentVersion(modulePom, rootReleaseVersion);
			}
		}
	}
}
