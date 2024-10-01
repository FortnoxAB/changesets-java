package se.fortnox.changesets;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.slf4j.LoggerFactory.getLogger;

public class ChangesetWriter {
	private static final Logger LOG = getLogger(ChangesetWriter.class);
	public static final String CHANGESET_DIR = ".changeset";

	private final Path baseDir;
	private final NameGenerator nameGenerator;

	public ChangesetWriter(Path baseDir) {
		this(baseDir, new RandomNameGenerator());
	}

	/**
	 * @param baseDir The base directory that is expected to already contain the .changesets folder,
	 *                or to have it created as a changeset is added
	 */
	ChangesetWriter(Path baseDir, NameGenerator nameGenerator) {
		this.baseDir = baseDir;
		this.nameGenerator = nameGenerator;
	}

	public void writeChangeset(Changeset changeset) throws FileAlreadyExistsException {
		writeChangeset(changeset.packageName(), changeset.level(), changeset.message());
	}

	Path writeChangeset(String packageName, Level changeLevel, String message) throws FileAlreadyExistsException {
		final String fileContent;
		if(message == null) {
			fileContent = """
			---
			"%s": %s
			---
			
			This is a template changelog entry created by changesets:add
			""".formatted(packageName, changeLevel.getTextValue());
		} else {
			fileContent = """
			---
			"%s": %s
			---
			
			%s""".formatted(packageName, changeLevel.getTextValue(), message);
		}

		Path changesetsDir = this.baseDir.resolve(CHANGESET_DIR);
		if (!changesetsDir.toFile().exists()) {
			try {
				Files.createDirectory(changesetsDir);
			} catch (IOException e) {
				LOG.error("Failed to create {}", changesetsDir, e);
			}
		}

		String newFileName = this.nameGenerator.humanId() + ".md";
		Path changesetFile = changesetsDir.resolve(newFileName);

		// Re-generate new name if the file already exists,
		// but a limited number of times before giving up to avoid infinite loops situations
		int attempt = 1;
		while (Files.exists(changesetFile)) {
			newFileName = this.nameGenerator.humanId() + ".md";
			LOG.debug("{} existed, trying again with new name {}", changesetFile, newFileName);
			changesetFile = changesetsDir.resolve(newFileName);

			attempt++;
			if (attempt >= 10) {
				LOG.error("Failed to generate a unique name after {} attempts", attempt);
				String string = changesetFile.toAbsolutePath().toString();
				throw new FileAlreadyExistsException(string, null, "Failed to generate a unique name after %s attempts".formatted(attempt));
			}
		}

		try {
			LOG.info("Writing changeset to {}", changesetFile);
			Files.writeString(changesetFile, fileContent, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			LOG.error("Failed to create new changeset", e);
		}

		return changesetFile;
	}
}
