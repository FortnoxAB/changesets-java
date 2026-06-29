package se.fortnox.changesets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.fortnox.changesets.ChangesetsConfig.ChangelogMode.ROOT;
import static se.fortnox.changesets.ChangesetsConfig.VersioningStrategy.FIXED;
import static se.fortnox.changesets.ChangesetsConfig.VersioningStrategy.INDEPENDENT;

class ChangesetsConfigTest {

	@Nested
	class Defaults {
		@Test
		void defaultsToFixedVersioningAndRootChangelog() {
			var config = ChangesetsConfig.defaults();

			assertThat(config.versioning()).isEqualTo(FIXED);
			assertThat(config.linked()).isEmpty();
			assertThat(config.fixed()).isEmpty();
			assertThat(config.changelog()).isEqualTo(ROOT);
		}

		@Test
		void canonicalConstructorFillsNullsWithDefaults() {
			var config = new ChangesetsConfig(null, null, null, null, null);

			assertThat(config.versioning()).isEqualTo(FIXED);
			assertThat(config.linked()).isEmpty();
			assertThat(config.fixed()).isEmpty();
			assertThat(config.changelog()).isEqualTo(ROOT);
			assertThat(config.bom()).isNull();
		}
	}

	@Nested
	class Loading {
		@TempDir
		Path tempDir;

		@Test
		void returnsDefaultsWhenConfigFileMissing() {
			var config = ChangesetsConfig.load(tempDir);

			assertThat(config).isEqualTo(ChangesetsConfig.defaults());
		}

		@Test
		void parsesFullyPopulatedConfig() throws IOException {
			Files.writeString(tempDir.resolve("config.json"), """
				{
				  "versioning": "independent",
				  "linked": [["pkg-a", "pkg-b"]],
				  "fixed": [["pkg-c", "pkg-d"]],
				  "changelog": "root",
				  "bom": {
				    "module": "pkg-bom",
				    "consumerParent": "pkg-parent"
				  }
				}
				""");

			var config = ChangesetsConfig.load(tempDir);

			assertThat(config.versioning()).isEqualTo(INDEPENDENT);
			assertThat(config.linked()).containsExactly(List.of("pkg-a", "pkg-b"));
			assertThat(config.fixed()).containsExactly(List.of("pkg-c", "pkg-d"));
			assertThat(config.changelog()).isEqualTo(ROOT);
			assertThat(config.bom().module()).isEqualTo("pkg-bom");
			assertThat(config.bom().consumerParent()).isEqualTo("pkg-parent");
		}

		@Test
		void parsesBomWithoutConsumerParent() throws IOException {
			Files.writeString(tempDir.resolve("config.json"), """
				{
				  "versioning": "independent",
				  "bom": { "module": "pkg-bom" }
				}
				""");

			var config = ChangesetsConfig.load(tempDir);

			assertThat(config.bom().module()).isEqualTo("pkg-bom");
			assertThat(config.bom().consumerParent()).isNull();
		}

		@Test
		void appliesDefaultsForMissingFields() throws IOException {
			Files.writeString(tempDir.resolve("config.json"), """
				{ "versioning": "independent" }
				""");

			var config = ChangesetsConfig.load(tempDir);

			assertThat(config.versioning()).isEqualTo(INDEPENDENT);
			assertThat(config.linked()).isEmpty();
			assertThat(config.fixed()).isEmpty();
			assertThat(config.changelog()).isEqualTo(ROOT);
		}

		@Test
		void returnsDefaultsOnMalformedJson() throws IOException {
			Files.writeString(tempDir.resolve("config.json"), "not json {");

			var config = ChangesetsConfig.load(tempDir);

			assertThat(config).isEqualTo(ChangesetsConfig.defaults());
		}
	}

	@Nested
	class Validation {
		@Test
		void rejectsModuleInTwoLinkedGroups() {
			assertThatThrownBy(() -> new ChangesetsConfig(
				INDEPENDENT,
				List.of(List.of("pkg-a", "pkg-b"), List.of("pkg-a", "pkg-c")),
				List.of(),
				ROOT,
				null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("pkg-a");
		}

		@Test
		void rejectsModuleInBothLinkedAndFixed() {
			assertThatThrownBy(() -> new ChangesetsConfig(
				INDEPENDENT,
				List.of(List.of("pkg-a", "pkg-b")),
				List.of(List.of("pkg-a", "pkg-c")),
				ROOT,
				null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("pkg-a");
		}

		@Test
		void allowsDistinctGroups() {
			var config = new ChangesetsConfig(
				INDEPENDENT,
				List.of(List.of("pkg-a", "pkg-b")),
				List.of(List.of("pkg-c", "pkg-d")),
				ROOT,
				null);

			assertThat(config.linked()).hasSize(1);
			assertThat(config.fixed()).hasSize(1);
		}
	}
}
