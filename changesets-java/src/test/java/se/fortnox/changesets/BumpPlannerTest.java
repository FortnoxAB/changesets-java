package se.fortnox.changesets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static se.fortnox.changesets.ChangesetsConfig.ChangelogMode.ROOT;
import static se.fortnox.changesets.ChangesetsConfig.VersioningStrategy.FIXED;
import static se.fortnox.changesets.ChangesetsConfig.VersioningStrategy.INDEPENDENT;

class BumpPlannerTest {

	private static Changeset changeset(String packageName, Level level) {
		return new Changeset(packageName, level, "msg", new File("dummy.md"));
	}

	@Nested
	class FixedStrategy {
		@Test
		void allModulesBumpTogetherOnAnyChangeset() {
			var reactor = Map.of("root", "1.0.0", "m1", "1.0.0", "m2", "1.0.0");
			var changes = List.of(changeset("m1", Level.MINOR));

			var result = BumpPlanner.plan(changes, reactor, ChangesetsConfig.defaults());

			assertThat(result).hasSize(3);
			assertThat(result.get("root").newVersion()).isEqualTo("1.1.0");
			assertThat(result.get("m1").newVersion()).isEqualTo("1.1.0");
			assertThat(result.get("m2").newVersion()).isEqualTo("1.1.0");
		}

		@Test
		void noChangesetsProducesEmptyResult() {
			var reactor = Map.of("root", "1.0.0", "m1", "1.0.0");

			var result = BumpPlanner.plan(List.of(), reactor, ChangesetsConfig.defaults());

			assertThat(result).isEmpty();
		}

		@Test
		void highestBumpLevelWinsAcrossAllChangesets() {
			var reactor = Map.of("root", "1.0.0", "m1", "1.0.0");
			var changes = List.of(
				changeset("m1", Level.PATCH),
				changeset("root", Level.MAJOR));

			var result = BumpPlanner.plan(changes, reactor, ChangesetsConfig.defaults());

			assertThat(result.get("m1").newVersion()).isEqualTo("2.0.0");
			assertThat(result.get("root").newVersion()).isEqualTo("2.0.0");
		}

		@Test
		void baseVersionIsMaxOfReactorWhenVersionsDiffer() {
			var reactor = Map.of("root", "1.0.0", "m1", "2.0.0", "m2", "1.5.0");
			var changes = List.of(changeset("m1", Level.PATCH));

			var result = BumpPlanner.plan(changes, reactor, ChangesetsConfig.defaults());

			assertThat(result.values()).allMatch(b -> b.newVersion().equals("2.0.1"));
		}
	}

	@Nested
	class IndependentStrategy {
		@Test
		void modulesWithoutGroupsBumpIndependently() {
			var reactor = Map.of("m1", "1.0.0", "m2", "2.0.0");
			var changes = List.of(
				changeset("m1", Level.MINOR),
				changeset("m2", Level.PATCH));
			var config = new ChangesetsConfig(INDEPENDENT, List.of(), List.of(), ROOT, null);

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result.get("m1").newVersion()).isEqualTo("1.1.0");
			assertThat(result.get("m2").newVersion()).isEqualTo("2.0.1");
		}

		@Test
		void modulesWithoutChangesetsAreOmitted() {
			var reactor = Map.of("m1", "1.0.0", "m2", "1.0.0");
			var changes = List.of(changeset("m1", Level.PATCH));
			var config = new ChangesetsConfig(INDEPENDENT, List.of(), List.of(), ROOT, null);

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result).containsOnlyKeys("m1");
			assertThat(result.get("m1").newVersion()).isEqualTo("1.0.1");
		}
	}

	@Nested
	class LinkedGroups {
		@Test
		void npmDocsExample_pkgA_patch_pkgB_minor_bothAt1_0_0() {
			// From changesets.dev/guide/linked-packages Release 1
			var reactor = Map.of("pkg-a", "1.0.0", "pkg-b", "1.0.0", "pkg-c", "1.0.0");
			var changes = List.of(
				changeset("pkg-a", Level.PATCH),
				changeset("pkg-b", Level.MINOR),
				changeset("pkg-c", Level.MAJOR));
			var config = new ChangesetsConfig(
				INDEPENDENT,
				List.of(List.of("pkg-a", "pkg-b")),
				List.of(),
				ROOT,
				null);

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result.get("pkg-a").newVersion()).isEqualTo("1.1.0");
			assertThat(result.get("pkg-b").newVersion()).isEqualTo("1.1.0");
			assertThat(result.get("pkg-c").newVersion()).isEqualTo("2.0.0");
		}

		@Test
		void onlyActiveMembersBumpInLinkedGroup() {
			var reactor = Map.of("pkg-a", "1.0.0", "pkg-b", "1.0.0");
			var changes = List.of(changeset("pkg-a", Level.MINOR));
			var config = new ChangesetsConfig(
				INDEPENDENT,
				List.of(List.of("pkg-a", "pkg-b")),
				List.of(),
				ROOT,
				null);

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result).containsOnlyKeys("pkg-a");
			assertThat(result.get("pkg-a").newVersion()).isEqualTo("1.1.0");
		}

		@Test
		void baseVersionIsMaxAcrossAllLinkedMembersIncludingInactive() {
			var reactor = Map.of("pkg-a", "1.0.0", "pkg-b", "2.5.0");
			var changes = List.of(changeset("pkg-a", Level.PATCH));
			var config = new ChangesetsConfig(
				INDEPENDENT,
				List.of(List.of("pkg-a", "pkg-b")),
				List.of(),
				ROOT,
				null);

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result).containsOnlyKeys("pkg-a");
			assertThat(result.get("pkg-a").newVersion()).isEqualTo("2.5.1");
		}
	}

	@Nested
	class FixedGroups {
		@Test
		void allFixedMembersBumpWhenAnyHasChangeset() {
			var reactor = Map.of("pkg-a", "1.0.0", "pkg-b", "1.0.0", "pkg-c", "1.0.0");
			var changes = List.of(changeset("pkg-a", Level.MINOR));
			var config = new ChangesetsConfig(
				INDEPENDENT,
				List.of(),
				List.of(List.of("pkg-a", "pkg-b")),
				ROOT,
				null);

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result.get("pkg-a").newVersion()).isEqualTo("1.1.0");
			assertThat(result.get("pkg-b").newVersion()).isEqualTo("1.1.0");
			assertThat(result).doesNotContainKey("pkg-c");
		}
	}

	@Nested
	class UnknownModules {
		@Test
		void changesetForUnknownModuleIsIgnored() {
			var reactor = Map.of("m1", "1.0.0");
			var changes = List.of(changeset("unknown", Level.PATCH));

			var result = BumpPlanner.plan(changes, reactor, ChangesetsConfig.defaults());

			assertThat(result).isEmpty();
		}
	}

	@Nested
	class BomBumping {
		@Test
		void bomBumpsAtMaxLevelOfTrackedModules() {
			var reactor = Map.of("bom", "1.0.0", "starter-a", "1.0.0", "starter-b", "1.0.0");
			var changes = List.of(
				changeset("starter-a", Level.PATCH),
				changeset("starter-b", Level.MINOR));
			var config = new ChangesetsConfig(
				INDEPENDENT,
				List.of(),
				List.of(),
				ROOT,
				new ChangesetsConfig.Bom("bom", null));

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result.get("bom").newVersion()).isEqualTo("1.1.0");
			assertThat(result.get("bom").changesets()).isEmpty();
			assertThat(result.get("starter-a").newVersion()).isEqualTo("1.0.1");
			assertThat(result.get("starter-b").newVersion()).isEqualTo("1.1.0");
		}

		@Test
		void bomKeepsExplicitChangesets() {
			var reactor = Map.of("bom", "1.0.0", "starter-a", "1.0.0");
			var changes = List.of(
				changeset("starter-a", Level.PATCH),
				changeset("bom", Level.MAJOR));
			var config = new ChangesetsConfig(
				INDEPENDENT,
				List.of(),
				List.of(),
				ROOT,
				new ChangesetsConfig.Bom("bom", null));

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result.get("bom").newVersion()).isEqualTo("2.0.0");
			assertThat(result.get("bom").changesets()).hasSize(1);
			assertThat(result.get("bom").changesets().get(0).level()).isEqualTo(Level.MAJOR);
		}

		@Test
		void consumerParentIsRemovedFromPlan() {
			var reactor = Map.of("bom", "1.0.0", "consumer-parent", "1.0.0", "starter-a", "1.0.0");
			var changes = List.of(changeset("starter-a", Level.PATCH));
			var config = new ChangesetsConfig(
				INDEPENDENT,
				List.of(),
				List.of(),
				ROOT,
				new ChangesetsConfig.Bom("bom", "consumer-parent"));

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result).doesNotContainKey("consumer-parent");
			assertThat(result).containsKeys("bom", "starter-a");
		}

		@Test
		void bomDoesNothingWhenNoTrackedModulesBumpAndNoExplicitChangesets() {
			var reactor = Map.of("bom", "1.0.0", "starter-a", "1.0.0");
			var changes = List.<Changeset>of();
			var config = new ChangesetsConfig(
				INDEPENDENT,
				List.of(),
				List.of(),
				ROOT,
				new ChangesetsConfig.Bom("bom", null));

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result).isEmpty();
		}

		@Test
		void dependencyOnlyChangesetDoesNotBumpBom() {
			var reactor = Map.of("bom", "1.0.0", "starter-a", "1.0.0");
			var changes = List.of(changeset("starter-a", Level.DEPENDENCY));
			var config = new ChangesetsConfig(
				INDEPENDENT,
				List.of(),
				List.of(),
				ROOT,
				new ChangesetsConfig.Bom("bom", null));

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result).containsOnlyKeys("starter-a");
		}
	}

	@Nested
	class DependencyOnly {
		@Test
		void dependencyOnlyChangesetEmitsBumpWithUnchangedVersion() {
			var reactor = Map.of("m1", "1.0.0");
			var changes = List.of(changeset("m1", Level.DEPENDENCY));
			var config = new ChangesetsConfig(INDEPENDENT, List.of(), List.of(), ROOT, null);

			var result = BumpPlanner.plan(changes, reactor, config);

			assertThat(result).containsOnlyKeys("m1");
			assertThat(result.get("m1").newVersion()).isEqualTo("1.0.0");
			assertThat(result.get("m1").isVersionChange()).isFalse();
		}
	}
}
