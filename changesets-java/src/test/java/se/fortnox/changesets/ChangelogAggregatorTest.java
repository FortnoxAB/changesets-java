package se.fortnox.changesets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;
import static se.fortnox.changesets.ChangelogAggregator.CHANGELOG_FILE;


class ChangelogAggregatorTest {

	public static final String PACKAGE_NAME = "my-package";

	@Test
	void shouldMergeChangesetIntoChangelog(@TempDir Path tempDir) throws FileAlreadyExistsException {
		ChangelogAggregator changelog = new ChangelogAggregator(tempDir);

		ChangesetWriter changesetWriter = new ChangesetWriter(tempDir);
		changesetWriter.writeChangeset(PACKAGE_NAME, Level.PATCH, "Hello!");

		assertThat(tempDir.resolve(ChangesetWriter.CHANGESET_DIR))
			.exists()
			.isDirectory()
			.isDirectoryContaining(path -> path.toFile().getName().endsWith(".md"));

		changelog.mergeChangesetsToChangelog(PACKAGE_NAME, "1.0.0");

		assertThat(tempDir.resolve(CHANGELOG_FILE))
			.exists()
			.isRegularFile()
			.content()
			.isEqualTo("""
				# my-package

				## 1.0.0

				### Patch Changes

				- Hello!
				""");

	}

	@Test
	void shouldSortLevelsAsMajorMinorPatch(@TempDir Path tempDir) throws FileAlreadyExistsException {
		ChangelogAggregator changelog = new ChangelogAggregator(tempDir);

		ChangesetWriter changesetWriter = new ChangesetWriter(tempDir);
		changesetWriter.writeChangeset(PACKAGE_NAME, Level.PATCH, "Lorem ipsum dolor sit amet");
		changesetWriter.writeChangeset(PACKAGE_NAME, Level.PATCH, "Consectetur adipiscing elit");
		changesetWriter.writeChangeset(PACKAGE_NAME, Level.MAJOR, "Sed aliquam diam eget arcu iaculis imperdiet");
		changesetWriter.writeChangeset(PACKAGE_NAME, Level.MAJOR, "Curabitur rhoncus urna convallis");
		changesetWriter.writeChangeset(PACKAGE_NAME, Level.MINOR, "Donec lobortis sodales posuere");
		changesetWriter.writeChangeset(PACKAGE_NAME, Level.MINOR, "Vestibulum eget porta felis");

		assertThat(tempDir.resolve(ChangesetWriter.CHANGESET_DIR))
			.exists()
			.isDirectory()
			.isDirectoryContaining(path -> path.toFile().getName().endsWith(".md"));

		changelog.mergeChangesetsToChangelog(PACKAGE_NAME, "1.0.0");

		assertThat(tempDir.resolve(CHANGELOG_FILE))
			.exists()
			.isRegularFile()
			.content()
			.isEqualTo("""
				# my-package
				
				## 1.0.0
				
				### Major Changes
				
				- Curabitur rhoncus urna convallis
				- Sed aliquam diam eget arcu iaculis imperdiet
				
				### Minor Changes
				
				- Donec lobortis sodales posuere
				- Vestibulum eget porta felis
				
				### Patch Changes
				
				- Consectetur adipiscing elit
				- Lorem ipsum dolor sit amet
				""");
	}

	@Test
	void shouldNotCreateChangelogIfNoChangesetsAreFound(@TempDir Path tempDir) {
		ChangelogAggregator changelog = new ChangelogAggregator(tempDir);
		changelog.mergeChangesetsToChangelog(PACKAGE_NAME, "1.0.0");

		assertThat(tempDir.resolve(CHANGELOG_FILE)).doesNotExist();
	}

	@Test
	void shouldNotUpdateChangelogIfNoChangesetsAreFound(@TempDir Path tempDir) throws IOException {
		assertThat(tempDir.resolve(CHANGELOG_FILE)).doesNotExist();

		String originalContent = "# my-package";
		Files.writeString(tempDir.resolve(CHANGELOG_FILE), originalContent, StandardOpenOption.CREATE_NEW);

		ChangelogAggregator changelog = new ChangelogAggregator(tempDir);
		changelog.mergeChangesetsToChangelog(PACKAGE_NAME, "1.0.0");

		assertThat(tempDir.resolve(CHANGELOG_FILE))
			.exists()
			.content()
			.isEqualTo(originalContent);
	}

	@Test
	void shouldAppendToExistingChangelog(@TempDir Path tempDir) throws IOException {
		var existingChangeLogContent = """
			# my-package

			## 1.0.0

			### Patch Changes

			- Existing entry
			""";

		Files.writeString(tempDir.resolve(CHANGELOG_FILE), existingChangeLogContent, StandardOpenOption.CREATE_NEW);

		ChangelogAggregator changelog = new ChangelogAggregator(tempDir);

		ChangesetWriter changesetWriter = new ChangesetWriter(tempDir);
		changesetWriter.writeChangeset(PACKAGE_NAME, Level.PATCH, "Hello!");

		assertThat(tempDir.resolve(ChangesetWriter.CHANGESET_DIR))
			.exists()
			.isDirectory()
			.isDirectoryContaining(path -> path.toFile().getName().endsWith(".md"));

		changelog.mergeChangesetsToChangelog(PACKAGE_NAME, "1.0.1");

		assertThat(tempDir.resolve(CHANGELOG_FILE))
			.exists()
			.isRegularFile()
			.content()
			.isEqualTo("""
				# my-package
  
				## 1.0.1
				
				### Patch Changes
				
				- Hello!
				
				## 1.0.0
				
				### Patch Changes
				
				- Existing entry
				""");
	}
}