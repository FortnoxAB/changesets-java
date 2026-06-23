package se.fortnox.changesets.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import se.fortnox.changesets.BumpPlanner;
import se.fortnox.changesets.BumpPlanner.ModuleBump;
import se.fortnox.changesets.ChangelogAggregator;
import se.fortnox.changesets.ChangelogAggregator.ReleaseEntry;
import se.fortnox.changesets.Changeset;
import se.fortnox.changesets.ChangesetLocator;
import se.fortnox.changesets.ChangesetsConfig;
import se.fortnox.changesets.VersionCalculator;
import se.fortnox.changesets.VersionsFile;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;

/**
 * Applies all changesets into the changelog, calculates new versions per module, and updates
 * submodule poms. Runs once at the reactor root.
 * <p>
 * Versioning strategy is read from {@code .changeset/config.json}; defaults to {@code fixed}.
 */
@Mojo(name = "prepare", defaultPhase = LifecyclePhase.INITIALIZE, aggregator = true)
public class PrepareMojo extends AbstractMojo {
	private final MavenProject project;
	private final MavenSession session;
	private final Logger logger;

	@Inject
	public PrepareMojo(MavenProject project, MavenSession session, Logger logger) {
		this.project = project;
		this.session = session;
		this.logger = logger;
	}

	/**
	 * Set to true in order to just process changeset files, avoiding any changes to the POM(s).
	 */
	@Parameter(property = "useReleasePluginIntegration", defaultValue = "false")
	protected boolean useReleasePluginIntegration = false;

	public void execute() {
		Path reactorRoot = project.getBasedir().toPath();
		Path changesetDir = reactorRoot.resolve(CHANGESET_DIR);

		ChangesetsConfig config = ChangesetsConfig.load(changesetDir);
		logger.info("Versioning strategy: " + config.versioning());

		List<Changeset> changesets = new ChangesetLocator(reactorRoot).getAllChangesets();
		if (changesets.isEmpty()) {
			logger.info("No changesets found in " + changesetDir);
			return;
		}

		Map<String, String> reactor = collectReactorVersions();
		Map<String, ModuleBump> plan = BumpPlanner.plan(changesets, reactor, config);

		if (plan.isEmpty()) {
			logger.info("No changesets matched any reactor module");
			return;
		}

		Map<String, String> changedVersions = new LinkedHashMap<>();
		plan.values().stream()
			.filter(ModuleBump::isVersionChange)
			.forEach(bump -> changedVersions.put(bump.artifactId(), bump.newVersion()));

		if (!changedVersions.isEmpty()) {
			VersionsFile.write(reactorRoot, changedVersions);
			logger.info("Wrote " + VersionsFile.locate(reactorRoot) + " with " + changedVersions.size() + " entry/entries");
		}

		writeChangelog(reactorRoot, plan);

		if (useReleasePluginIntegration) {
			logger.info("Changesets processed, but not updating POMs due to useReleasePluginIntegration being set to true.");
			return;
		}

		applyPomVersions(plan);
	}

	private Map<String, String> collectReactorVersions() {
		Map<String, String> reactor = new LinkedHashMap<>();
		for (MavenProject p : session.getProjects()) {
			reactor.put(p.getArtifactId(), stripSnapshot(p.getVersion()));
		}
		return reactor;
	}

	private static String stripSnapshot(String version) {
		if (version == null) {
			return "0.0.0";
		}
		return version.endsWith("-SNAPSHOT")
			? version.substring(0, version.length() - "-SNAPSHOT".length())
			: version;
	}

	private void writeChangelog(Path reactorRoot, Map<String, ModuleBump> plan) {
		Map<String, ReleaseEntry> entries = new LinkedHashMap<>();
		for (ModuleBump bump : plan.values()) {
			entries.put(bump.artifactId(), new ReleaseEntry(
				bump.artifactId(),
				bump.newVersion(),
				bump.changesets()
			));
		}
		new ChangelogAggregator(reactorRoot).mergeReleaseToChangelog(entries);
	}

	private void applyPomVersions(Map<String, ModuleBump> plan) {
		Map<String, MavenProject> byArtifactId = new LinkedHashMap<>();
		for (MavenProject p : session.getProjects()) {
			byArtifactId.put(p.getArtifactId(), p);
		}

		String rootArtifactId = project.getArtifactId();
		String rootNewVersion = null;

		for (ModuleBump bump : plan.values()) {
			if (!bump.isVersionChange()) {
				continue;
			}
			MavenProject moduleProject = byArtifactId.get(bump.artifactId());
			if (moduleProject == null) {
				continue;
			}
			String snapshotVersion = VersionCalculator.nextDevelopmentVersion(bump.newVersion());
			File pomFile = moduleProject.getFile();
			logger.info("Updating " + pomFile + " to " + snapshotVersion);
			PomUpdater.setProjectVersion(pomFile, snapshotVersion);

			if (bump.artifactId().equals(rootArtifactId)) {
				rootNewVersion = snapshotVersion;
			}
		}

		if (rootNewVersion != null) {
			syncParentReferencesInSubmodules(rootNewVersion);
		}
	}

	private void syncParentReferencesInSubmodules(String rootSnapshotVersion) {
		Path baseDir = project.getBasedir().toPath();
		for (String module : project.getModules()) {
			File modulePom = baseDir.resolve(module).resolve("pom.xml").toFile();
			logger.info("Updating submodule " + modulePom + " parent ref to " + rootSnapshotVersion);
			PomUpdater.setProjectParentVersion(modulePom, rootSnapshotVersion);
		}
	}
}
