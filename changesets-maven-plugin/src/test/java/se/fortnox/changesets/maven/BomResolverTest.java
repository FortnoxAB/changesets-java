package se.fortnox.changesets.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class BomResolverTest {

	@Test
	void mapsReactorArtifactsToPinningProperties() {
		Model model = new Model();
		DependencyManagement dm = new DependencyManagement();
		dm.addDependency(dep("com.example", "module-a", "${module-a.version}"));
		dm.addDependency(dep("com.example", "module-b", "${module-b.version}"));
		model.setDependencyManagement(dm);
		MavenProject bom = new MavenProject();
		bom.setOriginalModel(model);

		Map<String, String> result = BomResolver.resolvePinnedProperties(bom, reactorIds(
			"com.example:module-a", "module-a",
			"com.example:module-b", "module-b"
		));

		assertThat(result).containsExactly(
			entry("module-a", "module-a.version"),
			entry("module-b", "module-b.version")
		);
	}

	@Test
	void ignoresDependenciesNotInReactor() {
		Model model = new Model();
		DependencyManagement dm = new DependencyManagement();
		dm.addDependency(dep("com.example", "module-a", "${module-a.version}"));
		dm.addDependency(dep("org.thirdparty", "some-lib", "${thirdparty.version}"));
		model.setDependencyManagement(dm);
		MavenProject bom = new MavenProject();
		bom.setOriginalModel(model);

		Map<String, String> result = BomResolver.resolvePinnedProperties(bom, reactorIds(
			"com.example:module-a", "module-a"
		));

		assertThat(result).containsOnlyKeys("module-a");
	}

	@Test
	void ignoresDependenciesWithLiteralVersion() {
		Model model = new Model();
		DependencyManagement dm = new DependencyManagement();
		dm.addDependency(dep("com.example", "module-a", "1.2.3"));
		dm.addDependency(dep("com.example", "module-b", "${module-b.version}"));
		model.setDependencyManagement(dm);
		MavenProject bom = new MavenProject();
		bom.setOriginalModel(model);

		Map<String, String> result = BomResolver.resolvePinnedProperties(bom, reactorIds(
			"com.example:module-a", "module-a",
			"com.example:module-b", "module-b"
		));

		assertThat(result).containsOnlyKeys("module-b");
	}

	@Test
	void ignoresDependenciesWithoutVersion() {
		Model model = new Model();
		DependencyManagement dm = new DependencyManagement();
		Dependency d = new Dependency();
		d.setGroupId("com.example");
		d.setArtifactId("module-a");
		// no version set
		dm.addDependency(d);
		model.setDependencyManagement(dm);
		MavenProject bom = new MavenProject();
		bom.setOriginalModel(model);

		Map<String, String> result = BomResolver.resolvePinnedProperties(bom, reactorIds(
			"com.example:module-a", "module-a"
		));

		assertThat(result).isEmpty();
	}

	@Test
	void returnsEmptyWhenDependencyManagementMissing() {
		Model model = new Model();
		MavenProject bom = new MavenProject();
		bom.setOriginalModel(model);

		Map<String, String> result = BomResolver.resolvePinnedProperties(bom, reactorIds(
			"com.example:module-a", "module-a"
		));

		assertThat(result).isEmpty();
	}

	@Test
	void returnsEmptyWhenOriginalModelMissing() {
		MavenProject bom = new MavenProject();
		// originalModel not set

		Map<String, String> result = BomResolver.resolvePinnedProperties(bom, reactorIds(
			"com.example:module-a", "module-a"
		));

		assertThat(result).isEmpty();
	}

	@Test
	void ignoresPropertyRefEmbeddedInLargerVersionString() {
		// only bare `${prop}` versions get treated as pinning
		Model model = new Model();
		DependencyManagement dm = new DependencyManagement();
		dm.addDependency(dep("com.example", "module-a", "${module-a.version}.RELEASE"));
		model.setDependencyManagement(dm);
		MavenProject bom = new MavenProject();
		bom.setOriginalModel(model);

		Map<String, String> result = BomResolver.resolvePinnedProperties(bom, reactorIds(
			"com.example:module-a", "module-a"
		));

		assertThat(result).isEmpty();
	}

	@Test
	void preservesInsertionOrder() {
		Model model = new Model();
		DependencyManagement dm = new DependencyManagement();
		dm.addDependency(dep("com.example", "module-c", "${c.version}"));
		dm.addDependency(dep("com.example", "module-a", "${a.version}"));
		dm.addDependency(dep("com.example", "module-b", "${b.version}"));
		model.setDependencyManagement(dm);
		MavenProject bom = new MavenProject();
		bom.setOriginalModel(model);

		Map<String, String> result = BomResolver.resolvePinnedProperties(bom, reactorIds(
			"com.example:module-a", "module-a",
			"com.example:module-b", "module-b",
			"com.example:module-c", "module-c"
		));

		assertThat(result.keySet()).containsExactly("module-c", "module-a", "module-b");
	}

	private static Dependency dep(String groupId, String artifactId, String version) {
		Dependency d = new Dependency();
		d.setGroupId(groupId);
		d.setArtifactId(artifactId);
		d.setVersion(version);
		return d;
	}

	private static Map<String, String> reactorIds(String... kv) {
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			map.put(kv[i], kv[i + 1]);
		}
		return map;
	}
}
