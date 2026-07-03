package se.fortnox.changesets.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import se.fortnox.changesets.BumpPlanner;
import se.fortnox.changesets.BumpPlanner.ModuleBump;
import se.fortnox.changesets.ChangelogAggregator;
import se.fortnox.changesets.ChangelogAggregator.BomContext;
import se.fortnox.changesets.ChangelogAggregator.ReleaseEntry;
import se.fortnox.changesets.Changeset;
import se.fortnox.changesets.ChangesetLocator;
import se.fortnox.changesets.ChangesetsConfig;
import se.fortnox.changesets.ChangesetsConfig.Bom;
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

	/**
	 * Per-invocation override for BOM behavior. When {@code true}, any {@code bom}
	 * configuration in {@code .changeset/config.json} is ignored for this run —
	 * the BOM is not auto-bumped, its {@code <properties>} are not rewritten, and
	 * the changelog is rendered in plain multi-module mode (no consumer-parent
	 * wrapper header). Use this when you want to release a starter or two without
	 * cutting a new BOM version.
	 */
	@Parameter(property = "skipBom", defaultValue = "false")
	protected boolean skipBom = false;

	public void execute() throws MojoExecutionException {
		Path reactorRoot = project.getBasedir().toPath();
		Path changesetDir = reactorRoot.resolve(CHANGESET_DIR);

		ChangesetsConfig loadedConfig = ChangesetsConfig.load(changesetDir);
		ChangesetsConfig config = applySkipBom(loadedConfig);
		logger.info("Versioning strategy: " + config.versioning());

		Map<String, MavenProject> byArtifactId = collectProjectsByArtifactId();
		validateBomConfig(config.bom(), byArtifactId);

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

		writeChangelog(reactorRoot, plan, config.bom(), byArtifactId);

		if (useReleasePluginIntegration) {
			logger.info("Changesets processed, but not updating POMs due to useReleasePluginIntegration being set to true.");
			return;
		}

		Map<String, String> snapshotVersions = applyPomVersions(plan, byArtifactId);
		if (config.bom() != null) {
			applyBomPropertyUpdates(config.bom(), plan, snapshotVersions, byArtifactId);
		}
	}

	private ChangesetsConfig applySkipBom(ChangesetsConfig config) {
		if (!skipBom || config.bom() == null) {
			return config;
		}
		logger.info("skipBom=true: ignoring BOM config '" + config.bom().module() + "' for this prepare run");
		return new ChangesetsConfig(config.versioning(), config.linked(), config.fixed(), config.changelog(), null);
	}

	private Map<String, MavenProject> collectProjectsByArtifactId() {
		Map<String, MavenProject> byArtifactId = new LinkedHashMap<>();
		for (MavenProject p : session.getProjects()) {
			byArtifactId.put(p.getArtifactId(), p);
		}
		return byArtifactId;
	}

	private void validateBomConfig(Bom bom, Map<String, MavenProject> byArtifactId) throws MojoExecutionException {
		if (bom == null) {
			return;
		}
		if (!byArtifactId.containsKey(bom.module())) {
			throw new MojoExecutionException(
				"bom.module '" + bom.module() + "' is not present in the reactor");
		}
		if (bom.consumerParent() != null) {
			MavenProject cp = byArtifactId.get(bom.consumerParent());
			if (cp == null) {
				throw new MojoExecutionException(
					"bom.consumerParent '" + bom.consumerParent() + "' is not present in the reactor");
			}
			String ownVersion = cp.getOriginalModel() == null ? null : cp.getOriginalModel().getVersion();
			MavenProject bomProject = byArtifactId.get(bom.module());
			String bomVersion = bomProject.getOriginalModel() == null ? null : bomProject.getOriginalModel().getVersion();
			if (ownVersion != null && !ownVersion.equals(bomVersion)) {
				throw new MojoExecutionException(
					"bom.consumerParent '" + bom.consumerParent() + "' has its own <version> ("
						+ ownVersion + ") different from the BOM's (" + bomVersion + "); "
						+ "consumer-parent must inherit its version from the BOM");
			}
		}
	}

	private Map<String, String> collectReactorVersions() {
		Map<String, String> reactor = new LinkedHashMap<>();
		for (MavenProject p : session.getProjects()) {
			reactor.put(p.getArtifactId(), p.getVersion() == null ? "0.0.0" : p.getVersion());
		}
		return reactor;
	}

	private void writeChangelog(Path reactorRoot, Map<String, ModuleBump> plan, Bom bom, Map<String, MavenProject> byArtifactId) {
		Map<String, ReleaseEntry> entries = new LinkedHashMap<>();
		for (ModuleBump bump : plan.values()) {
			entries.put(bump.artifactId(), new ReleaseEntry(
				bump.artifactId(),
				bump.newVersion(),
				bump.changesets()
			));
		}

		BomContext bomContext = null;
		if (bom != null && plan.containsKey(bom.module())) {
			ModuleBump bomBump = plan.get(bom.module());
			String headerArtifactId = bom.consumerParent() != null ? bom.consumerParent() : bom.module();
			Map<String, String> pinnedUpdates = collectPinnedUpdates(bom, plan, byArtifactId);
			bomContext = new BomContext(headerArtifactId, bomBump.newVersion(), bom.module(), pinnedUpdates);
		}

		new ChangelogAggregator(reactorRoot).mergeReleaseToChangelog(entries, bomContext);
	}

	private Map<String, String> collectPinnedUpdates(Bom bom, Map<String, ModuleBump> plan, Map<String, MavenProject> byArtifactId) {
		MavenProject bomProject = byArtifactId.get(bom.module());
		Map<String, String> reactorIdsByGav = new LinkedHashMap<>();
		for (MavenProject p : session.getProjects()) {
			reactorIdsByGav.put(p.getGroupId() + ":" + p.getArtifactId(), p.getArtifactId());
		}
		Map<String, String> pinnedProps = BomResolver.resolvePinnedProperties(bomProject, reactorIdsByGav);

		Map<String, String> updates = new LinkedHashMap<>();
		for (String artifactId : pinnedProps.keySet()) {
			ModuleBump bump = plan.get(artifactId);
			if (bump != null && bump.isVersionChange()) {
				updates.put(artifactId, bump.newVersion());
			}
		}
		return updates;
	}

	/**
	 * Writes each bumped module's pom to its next development (snapshot) version, and
	 * keeps parent references in sync when their parent is also bumped. Returns the
	 * artifactId → snapshotVersion map for downstream use (e.g. BOM property rewrite).
	 */
	private Map<String, String> applyPomVersions(Map<String, ModuleBump> plan, Map<String, MavenProject> byArtifactId) {
		Map<String, String> snapshotVersions = new LinkedHashMap<>();

		for (ModuleBump bump : plan.values()) {
			if (!bump.isVersionChange()) {
				continue;
			}
			MavenProject moduleProject = byArtifactId.get(bump.artifactId());
			if (moduleProject == null) {
				continue;
			}
			String snapshotVersion = VersionCalculator.nextDevelopmentVersion(bump.newVersion());
			snapshotVersions.put(bump.artifactId(), snapshotVersion);
			File pomFile = moduleProject.getFile();
			logger.info("Updating " + pomFile + " to " + snapshotVersion);
			PomUpdater.setProjectVersion(pomFile, snapshotVersion);
		}

		syncParentReferences(snapshotVersions, byArtifactId);
		return snapshotVersions;
	}

	private void syncParentReferences(Map<String, String> snapshotVersions, Map<String, MavenProject> byArtifactId) {
		for (MavenProject p : session.getProjects()) {
			if (p.getOriginalModel() == null || p.getOriginalModel().getParent() == null) {
				continue;
			}
			String parentArtifactId = p.getOriginalModel().getParent().getArtifactId();
			String parentSnapshot = snapshotVersions.get(parentArtifactId);
			if (parentSnapshot == null) {
				continue;
			}
			File pomFile = p.getFile();
			logger.info("Updating " + pomFile + " parent ref to " + parentSnapshot);
			PomUpdater.setProjectParentVersion(pomFile, parentSnapshot);
		}
	}

	private void applyBomPropertyUpdates(
		Bom bom,
		Map<String, ModuleBump> plan,
		Map<String, String> snapshotVersions,
		Map<String, MavenProject> byArtifactId
	) {
		MavenProject bomProject = byArtifactId.get(bom.module());
		Map<String, String> reactorIdsByGav = new LinkedHashMap<>();
		for (MavenProject p : session.getProjects()) {
			reactorIdsByGav.put(p.getGroupId() + ":" + p.getArtifactId(), p.getArtifactId());
		}
		Map<String, String> pinnedProps = BomResolver.resolvePinnedProperties(bomProject, reactorIdsByGav);

		File bomPom = bomProject.getFile();
		for (Map.Entry<String, String> entry : pinnedProps.entrySet()) {
			String artifactId = entry.getKey();
			String propertyName = entry.getValue();
			ModuleBump bump = plan.get(artifactId);
			if (bump == null || !bump.isVersionChange()) {
				continue;
			}
			String snapshotVersion = snapshotVersions.get(artifactId);
			if (snapshotVersion == null) {
				continue;
			}
			logger.info("Updating " + bomPom + " property " + propertyName + " to " + snapshotVersion);
			PomUpdater.setProperty(bomPom, propertyName, snapshotVersion);
		}
	}
}
