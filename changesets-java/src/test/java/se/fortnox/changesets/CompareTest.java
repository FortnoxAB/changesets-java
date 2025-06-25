package se.fortnox.changesets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CompareTest {
	@Test
	void shouldGenerateChangelogForReactiveWizard(@TempDir Path tempDir) throws IOException {
		Path changesetsPath = Path.of("src/test/resources/changesets/reactive-wizard/.changeset");

		Path changesetsTarget = tempDir.resolve(".changeset");
		changesetsTarget.toFile().mkdir();

		String[] files = changesetsPath.toFile().list();
		for (String file : files) {
			Files.copy(changesetsPath.resolve(file), changesetsTarget.resolve(file));
		}

		assertThat(changesetsTarget.toFile().list())
			.isNotEmpty()
			.containsExactly(changesetsPath.toFile().list());

		ChangelogAggregator changelog = new ChangelogAggregator(tempDir);
		Path changelogFile = changelog.mergeChangesetsToChangelog("reactivewizard-parent", "26.0.0");

		String actualChangelogText = Files.readString(changelogFile);
		String expectedChangelogText = Files.readString(changesetsPath.resolve("../CHANGELOG-expected.md"));

		assertThat(actualChangelogText)
			.isEqualTo(expectedChangelogText);

	}
	@Test
	void shouldGenerateChangelogForReactiveWizardWithDependencyTypes(@TempDir Path tempDir) throws IOException {
		Path changesetsPath = Path.of("src/test/resources/changesets/reactive-wizard-with-dependencies/.changeset");

		Path changesetsTarget = tempDir.resolve(".changeset");
		changesetsTarget.toFile().mkdir();

		String[] files = changesetsPath.toFile().list();
		for (String file : files) {
			Files.copy(changesetsPath.resolve(file), changesetsTarget.resolve(file));
		}

		assertThat(changesetsTarget.toFile().list())
			.isNotEmpty()
			.containsExactly(changesetsPath.toFile().list());

		ChangelogAggregator changelog = new ChangelogAggregator(tempDir);
		Path changelogFile = changelog.mergeChangesetsToChangelog("reactivewizard-parent", "26.0.0");

		String actualChangelogText = Files.readString(changelogFile);
		String expectedChangelogText = Files.readString(changesetsPath.resolve("../CHANGELOG-expected.md"));

		assertThat(actualChangelogText)
			.isEqualTo(expectedChangelogText);

	}
}
