package se.fortnox.changesets.maven.policy;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChangesetsVersionPolicyTest {

	@TempDir
	Path reactorRoot;

	@Nested
	class GetReleaseVersion {

		@Test
		void returnsVersionFromVersionsFileStrippingSnapshot() throws Exception {
			writeVersions("""
				module-a=1.2.3
				module-b=2.0.0
				""");
			MavenProject rootProject = project("root", "1.0.0", reactorRoot);
			MavenProject moduleA = project("module-a", "1.0.0-SNAPSHOT", reactorRoot.resolve("module-a"));
			MavenProject moduleB = project("module-b", "2.0.0-SNAPSHOT", reactorRoot.resolve("module-b"));

			var result = policy(rootProject, session(List.of(rootProject, moduleA, moduleB)))
				.getReleaseVersion(request("1.0.0-SNAPSHOT"));

			assertThat(result.getVersion()).isEqualTo("1.2.3");
		}

		@Test
		void fallsBackToRequestVersionWhenModuleNotInVersionsFile() throws Exception {
			writeVersions("other-module=9.9.9\n");
			MavenProject rootProject = project("root", "1.0.0", reactorRoot);
			MavenProject moduleA = project("module-a", "1.4.6-SNAPSHOT", reactorRoot.resolve("module-a"));

			var result = policy(rootProject, session(List.of(rootProject, moduleA)))
				.getReleaseVersion(request("1.4.6-SNAPSHOT"));

			assertThat(result.getVersion()).isEqualTo("1.4.6");
		}

		@Test
		void fallsBackToRequestVersionWhenVersionsFileMissing() throws Exception {
			// no VERSIONS file created
			Files.createDirectories(reactorRoot.resolve(".changeset"));
			MavenProject rootProject = project("root", "2.0.1-SNAPSHOT", reactorRoot);

			var result = policy(rootProject, session(List.of(rootProject)))
				.getReleaseVersion(request("2.0.1-SNAPSHOT"));

			assertThat(result.getVersion()).isEqualTo("2.0.1");
		}

		@Test
		void singleModuleFlowResolvesWhenReactorHasOnlyRoot() throws Exception {
			writeVersions("my-package=3.1.4\n");
			MavenProject root = project("my-package", "3.0.0-SNAPSHOT", reactorRoot);

			var result = policy(root, session(List.of(root)))
				.getReleaseVersion(request("3.0.0-SNAPSHOT"));

			assertThat(result.getVersion()).isEqualTo("3.1.4");
		}
	}

	@Nested
	class GetDevelopmentVersion {

		@Test
		void returnsNextPatchSnapshotOfMappedVersion() throws Exception {
			writeVersions("module-a=1.2.3\n");
			MavenProject root = project("root", "1.0.0", reactorRoot);
			MavenProject moduleA = project("module-a", "1.0.0-SNAPSHOT", reactorRoot.resolve("module-a"));

			var result = policy(root, session(List.of(root, moduleA)))
				.getDevelopmentVersion(request("1.0.0-SNAPSHOT"));

			assertThat(result.getVersion()).isEqualTo("1.2.4-SNAPSHOT");
		}

		@Test
		void leavesUnmappedModuleUnchanged() throws Exception {
			// Modules absent from VERSIONS are not bumped, so multi-module reactors
			// don't inadvertently push a next-dev bump onto an aggregator root.
			writeVersions("other=1.0.0\n");
			MavenProject root = project("root", "1.0.0", reactorRoot);
			MavenProject moduleA = project("module-a", "2.5.0-SNAPSHOT", reactorRoot.resolve("module-a"));

			var result = policy(root, session(List.of(root, moduleA)))
				.getDevelopmentVersion(request("2.5.0-SNAPSHOT"));

			assertThat(result.getVersion()).isEqualTo("2.5.0-SNAPSHOT");
		}
	}

	@Nested
	class ModuleIdentification {

		@Test
		void identifiesModuleByCurrentVersionAcrossReactor() throws Exception {
			// The injected MavenProject is the aggregator root — the policy must resolve
			// the *per-module* artifactId from the reactor by matching request.getVersion().
			writeVersions("""
				module-a=2.1.0
				module-b=3.0.5
				""");
			MavenProject root = project("root", "1.0.0", reactorRoot);
			MavenProject moduleA = project("module-a", "2.0.5-SNAPSHOT", reactorRoot.resolve("module-a"));
			MavenProject moduleB = project("module-b", "3.0.5-SNAPSHOT", reactorRoot.resolve("module-b"));
			MavenSession session = session(List.of(root, moduleA, moduleB));

			var forA = policy(root, session).getReleaseVersion(request("2.0.5-SNAPSHOT"));
			var forB = policy(root, session).getReleaseVersion(request("3.0.5-SNAPSHOT"));

			assertThat(forA.getVersion()).isEqualTo("2.1.0");
			assertThat(forB.getVersion()).isEqualTo("3.0.5");
		}

		@Test
		void fallsBackWhenTwoReactorModulesShareCurrentVersion() throws Exception {
			// Ambiguous — two candidate modules; policy must not guess.
			writeVersions("""
				module-a=9.9.9
				module-b=8.8.8
				""");
			MavenProject root = project("root", "1.0.0", reactorRoot);
			MavenProject moduleA = project("module-a", "1.0.0-SNAPSHOT", reactorRoot.resolve("module-a"));
			MavenProject moduleB = project("module-b", "1.0.0-SNAPSHOT", reactorRoot.resolve("module-b"));

			var result = policy(root, session(List.of(root, moduleA, moduleB)))
				.getReleaseVersion(request("1.0.0-SNAPSHOT"));

			assertThat(result.getVersion()).isEqualTo("1.0.0");
		}
	}

	@Nested
	class ReactorRootLookup {

		@Test
		void prefersRequestWorkingDirectoryAsReactorHint() throws Exception {
			writeVersions("child=4.5.6\n");
			Path childDir = Files.createDirectories(reactorRoot.resolve("nested/deep/child"));
			MavenProject root = project("root", "1.0.0", reactorRoot);
			MavenProject child = project("child", "0.0.1-SNAPSHOT", childDir);

			var result = policyWithWorkingDir(root, session(List.of(root, child)), reactorRoot.toString())
				.getReleaseVersion(request("0.0.1-SNAPSHOT"));

			assertThat(result.getVersion()).isEqualTo("4.5.6");
		}
	}

	private void writeVersions(String contents) throws IOException {
		Path dir = Files.createDirectories(reactorRoot.resolve(".changeset"));
		Files.writeString(dir.resolve("VERSIONS"), contents);
	}

	private static MavenProject project(String artifactId, String version, Path basedir) {
		Model model = new Model();
		model.setGroupId("test");
		model.setArtifactId(artifactId);
		model.setVersion(version);
		MavenProject project = new MavenProject(model);
		project.setFile(basedir.resolve("pom.xml").toFile());
		return project;
	}

	private static MavenSession session(List<MavenProject> projects) {
		MavenSession session = mock(MavenSession.class);
		when(session.getProjects()).thenReturn(projects);
		return session;
	}

	private static ChangesetsVersionPolicy policy(MavenProject project, MavenSession session) {
		return new ChangesetsVersionPolicy(project, session);
	}

	private static VersionPolicyRequest request(String version) {
		return new VersionPolicyRequest().setVersion(version);
	}

	private static ChangesetsVersionPolicy policyWithWorkingDir(MavenProject project, MavenSession session, String ignored) {
		return new ChangesetsVersionPolicy(project, session);
	}
}
