package se.fortnox.changesets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static se.fortnox.changesets.Level.PATCH;

class ChangesetWriterTest {
	@Test
	void shouldCreateDirectoryIfNeeded(@TempDir Path tempDir) throws FileAlreadyExistsException {
		File changesetsDir = tempDir.resolve(ChangesetWriter.CHANGESET_DIR).toFile();
		assertThat(changesetsDir).doesNotExist();

		ChangesetWriter stuffer = new ChangesetWriter(tempDir);
		stuffer.writeChangeset("my-package", PATCH, "A test change");

		assertThat(changesetsDir).exists();
	}


	@Test
	void shouldCreateFileWithRandomName(@TempDir Path tempDir) throws FileAlreadyExistsException {
		String expectedFilename = "static-name";
		NameGenerator nameGenerator = new StaticNameGenerator(expectedFilename);
		ChangesetWriter stuffer = new ChangesetWriter(tempDir, nameGenerator);

		Path changesetFile = stuffer.writeChangeset("my-package", PATCH, "A test change");

		assertThat(changesetFile)
			.exists()
			.hasParent(tempDir.resolve(ChangesetWriter.CHANGESET_DIR))
			.hasFileName(expectedFilename + ".md")
			.hasContent("""
				---
				"my-package": patch
				---

				A test change""");
	}

	@Test
	void shouldAttemptToGenerateNewNameIfExists(@TempDir Path tempDir) throws IOException {
		NameGenerator nameGenerator = mock(NameGenerator.class);
		doReturn("already-exists")
			.doReturn("new-name")
			.when(nameGenerator)
			.humanId();

		ChangesetWriter stuffer = new ChangesetWriter(tempDir, nameGenerator);

		Files.createDirectory(tempDir.resolve(ChangesetWriter.CHANGESET_DIR));
		Files.writeString(tempDir.resolve(ChangesetWriter.CHANGESET_DIR).resolve("already-exists.md"), "");

		Path changesetFile = stuffer.writeChangeset("my-package", PATCH, "A test change");

		assertThat(changesetFile)
			.exists()
			.hasParent(tempDir.resolve(ChangesetWriter.CHANGESET_DIR))
			.hasFileName("new-name.md");
	}

	@Test
	void shouldGiveUpIfFailingToGenerateNewNames(@TempDir Path tempDir) throws IOException {
		NameGenerator nameGenerator = mock(NameGenerator.class);
		doReturn("already-exists")
			.when(nameGenerator)
			.humanId();
		ChangesetWriter stuffer = new ChangesetWriter(tempDir, nameGenerator);

		Files.createDirectory(tempDir.resolve(ChangesetWriter.CHANGESET_DIR));
		Files.writeString(tempDir.resolve(ChangesetWriter.CHANGESET_DIR).resolve("already-exists.md"), "");

		assertThatExceptionOfType(FileAlreadyExistsException.class)
			.isThrownBy(() -> {
				stuffer.writeChangeset("my-package", PATCH, "A test change");
			});

		verify(nameGenerator, times(10)).humanId();
	}


	@Test
	void shouldUseMessageTemplateIfNoMessageIsGiven(@TempDir Path tempDir) throws FileAlreadyExistsException {
		String expectedFilename = "static-name";
		NameGenerator nameGenerator = new StaticNameGenerator(expectedFilename);
		ChangesetWriter stuffer = new ChangesetWriter(tempDir, nameGenerator);

		Path changesetFile = stuffer.writeChangeset("my-package", PATCH, null);

		assertThat(changesetFile)
			.exists()
			.hasParent(tempDir.resolve(ChangesetWriter.CHANGESET_DIR))
			.hasFileName(expectedFilename + ".md")
			.content()
			.isEqualTo("""
				---
				"my-package": patch
				---

				This is a template changelog entry created by changesets:add
				""");
	}
}