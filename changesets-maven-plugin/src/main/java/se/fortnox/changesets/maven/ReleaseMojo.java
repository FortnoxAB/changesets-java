package se.fortnox.changesets.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import se.fortnox.changesets.ChangesetsConfig;
import se.fortnox.changesets.ChangesetsConfig.Bom;
import se.fortnox.changesets.VersionsFile;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;

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

		for (Map.Entry<String, String> entry : versions.entrySet()) {
			MavenProject moduleProject = byArtifactId.get(entry.getKey());
			if (moduleProject == null) {
				log.warn("VERSIONS entry for unknown module: " + entry.getKey());
				continue;
			}
			File pomFile = moduleProject.getFile();
			log.info("Updating " + pomFile + " to " + entry.getValue());
			PomUpdater.setProjectVersion(pomFile, entry.getValue());
		}

		syncParentReferences(versions, byArtifactId);

		ChangesetsConfig config = ChangesetsConfig.load(reactorRoot.resolve(CHANGESET_DIR));
		if (config.bom() != null && versions.containsKey(config.bom().module())) {
			applyBomPropertyUpdates(config.bom(), versions, byArtifactId);
		} else if (config.bom() != null) {
			log.info("BOM '" + config.bom().module() + "' not in VERSIONS — skipping BOM property updates");
		}
	}

	private void syncParentReferences(Map<String, String> versions, Map<String, MavenProject> byArtifactId) {
		for (MavenProject p : session.getProjects()) {
			if (p.getOriginalModel() == null || p.getOriginalModel().getParent() == null) {
				continue;
			}
			String parentArtifactId = p.getOriginalModel().getParent().getArtifactId();
			String parentReleaseVersion = versions.get(parentArtifactId);
			if (parentReleaseVersion == null) {
				continue;
			}
			File pomFile = p.getFile();
			log.info("Updating " + pomFile + " parent ref to " + parentReleaseVersion);
			PomUpdater.setProjectParentVersion(pomFile, parentReleaseVersion);
		}
	}

	private void applyBomPropertyUpdates(Bom bom, Map<String, String> versions, Map<String, MavenProject> byArtifactId) {
		MavenProject bomProject = byArtifactId.get(bom.module());
		if (bomProject == null) {
			return;
		}
		Map<String, String> reactorIdsByGav = new LinkedHashMap<>();
		for (MavenProject p : session.getProjects()) {
			reactorIdsByGav.put(p.getGroupId() + ":" + p.getArtifactId(), p.getArtifactId());
		}
		Map<String, String> pinnedProps = BomResolver.resolvePinnedProperties(bomProject, reactorIdsByGav);

		File bomPom = bomProject.getFile();
		for (Map.Entry<String, String> entry : pinnedProps.entrySet()) {
			String artifactId = entry.getKey();
			String propertyName = entry.getValue();
			String releaseVersion = versions.get(artifactId);
			if (releaseVersion == null) {
				continue;
			}
			log.info("Updating " + bomPom + " property " + propertyName + " to " + releaseVersion);
			PomUpdater.setProperty(bomPom, propertyName, releaseVersion);
		}
	}
}
