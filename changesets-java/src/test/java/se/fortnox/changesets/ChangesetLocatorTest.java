package se.fortnox.changesets;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static se.fortnox.changesets.Level.MAJOR;
import static se.fortnox.changesets.Level.MINOR;
import static se.fortnox.changesets.Level.PATCH;

class ChangesetLocatorTest {
	@Test
	void shouldOnlyReturnChangesetsMatchingRequestedPackage() {
		ChangesetLocator gatherer = new ChangesetLocator(Path.of("src/test/resources/changesets/multiple-packages"));

		List<Changeset> changesets = gatherer.getChangesets("my-package");
		assertThat(changesets)
			.hasSize(2)
			.allSatisfy(changeset -> assertThat(changeset.packageName()).isEqualTo("my-package"));

	}

	@Test
	void shouldMapLevelsCorrectly() {
		ChangesetLocator gatherer = new ChangesetLocator(Path.of("src/test/resources/changesets/mixed-levels"));

		List<Changeset> changesets = gatherer.getChangesets("my-package");
		assertThat(changesets)
			.hasSize(3)
			.allSatisfy(changeset -> assertThat(changeset.packageName()).isEqualTo("my-package"))
			.anySatisfy(changeset -> {
				assertThat(changeset.level()).isEqualTo(MAJOR);
				assertThat(changeset.message()).isEqualTo("A great change");
			})
			.anySatisfy(changeset -> {
				assertThat(changeset.level()).isEqualTo(MINOR);
				assertThat(changeset.message()).isEqualTo("A medium change");
			})
			.anySatisfy(changeset -> {
				assertThat(changeset.level()).isEqualTo(PATCH);
				assertThat(changeset.message()).isEqualTo("A tiny change");
			});
	}

}