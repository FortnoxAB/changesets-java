package se.fortnox.changesets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VersionCalculatorTest {

	@Test
	void bumpMajor() {
		var changeset = new Changeset("package", Level.MAJOR, "Test", mock(File.class));
		assertThat(VersionCalculator.getNewVersion("1.0.0", List.of(changeset))).isEqualTo("2.0.0");
	}

	@Test
	void bumpMinor() {
		var changeset = new Changeset("package", Level.MINOR, "Test", mock(File.class));
		assertThat(VersionCalculator.getNewVersion("1.0.0", List.of(changeset))).isEqualTo("1.1.0");
	}

	@Test
	void bumpPatch() {
		var changeset = new Changeset("package", Level.PATCH, "Test", mock(File.class));
		assertThat(VersionCalculator.getNewVersion("1.0.0", List.of(changeset))).isEqualTo("1.0.1");
	}

	@Test
	void bumpMajorResetsMinorAndPatch() {
		var changeset = new Changeset("package", Level.MAJOR, "Test", mock(File.class));
		assertThat(VersionCalculator.getNewVersion("1.1.1", List.of(changeset))).isEqualTo("2.0.0");
	}

	@Test
	void bumpMinorResetsPatch() {
		var changeset = new Changeset("package", Level.MINOR, "Test", mock(File.class));
		assertThat(VersionCalculator.getNewVersion("1.1.1", List.of(changeset))).isEqualTo("1.2.0");
	}

	@Nested
	class BuildAndPrereleaseMeta {
		@Test
		void bumpingResetsPreReleaseMeta() {
			var changeset = new Changeset("package", Level.PATCH, "Test", mock(File.class));
			assertThat(VersionCalculator.getNewVersion("1.0.1-SNAPSHOT", List.of(changeset))).isEqualTo("1.0.1");
		}

		@Test
		void bumpingResetsBuildMeta() {
			var changeset = new Changeset("package", Level.PATCH, "Test", mock(File.class));
			assertThat(VersionCalculator.getNewVersion("1.1.1+build", List.of(changeset))).isEqualTo("1.1.2");
		}

		@Test
		void bumpingResetsBuildAndPrereleaseMeta() {
			var changeset = new Changeset("package", Level.PATCH, "Test", mock(File.class));
			assertThat(VersionCalculator.getNewVersion("1.1.1+build-SNAPSHOT", List.of(changeset))).isEqualTo("1.1.2");
		}
	}


}