package se.fortnox.changesets;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;
import static se.fortnox.changesets.Level.MAJOR;
import static se.fortnox.changesets.Level.MINOR;
import static se.fortnox.changesets.Level.PATCH;

public class ChangelogAggregator {
	private static final Logger LOG = getLogger(ChangelogAggregator.class);
	public static final String CHANGELOG_FILE = "CHANGELOG.md";
	private final Path baseDir;

	public ChangelogAggregator(Path baseDir) {
		this.baseDir = baseDir;
	}

	/**
	 * Merge any changesets in baseDir matching the package name into CHANGELOG.md,
	 * creating it if it exists or prepending it to the existing content.
	 *
	 * @param packageName The package name to get changesets for
	 * @param version The version number of the merged changes
	 */
	public void mergeChangesetsToChangelog(String packageName, String version) {
		Path changesetsDir = this.baseDir.resolve(CHANGESET_DIR);

		ChangesetLocator changesetLocator = new ChangesetLocator(this.baseDir);
		List<Changeset> changesets = changesetLocator.getChangesets(packageName);
		if (changesets.isEmpty()) {
			LOG.info("No changesets found in {}", this.baseDir);
			return;
		}

		String changelog = generateChangelog(packageName, version, changesets);

		try {
			writeChangelog(changelog);
		} catch (ChangelogException exception) {
			LOG.error("Failed to update changelog at {}", changesetsDir, exception);
		}

		changesets.forEach(changeset -> {
			File file = changeset.file();
			try {
				Files.delete(file.toPath());
			} catch (IOException e) {
				LOG.error("Failed to delete {}", file, e);
			}
		});
	}

	private static String generateChangelog(String packageName, String version, List<Changeset> changesets) {
		String changes = changesets
			.stream()
			.collect(groupingBy(Changeset::level, mapping(Changeset::message, toList())))
			.entrySet()
			.stream()
			.sorted(sortChangesets())
			.map(entry -> {
				String level = entry.getKey().getPresentationString();
				String levelChanges = entry.getValue().stream()
					.map(change -> "- " + change.trim())
					.sorted()
					.collect(Collectors.joining("\n"));

				return """
					### %s Changes
					
					%s""".formatted(level, levelChanges);
			})
			.collect(Collectors.joining("\n\n"));

		return """
			# %s
			
			## %s
			
			%s
			""".formatted(packageName, version, changes);
	}

	private static Comparator<Map.Entry<Level, List<String>>> sortChangesets() {
		List<Level> levelOrder = List.of(MAJOR, MINOR, PATCH);

		return (o1, o2) -> {
			// Sort levels in the order specified in levelOrder
			if (o1.getKey() == o2.getKey()) {
				return 0;
			}
			return levelOrder.indexOf(o1.getKey()) < levelOrder.indexOf(o2.getKey()) ? -1 : 1;
		};
	}

	/**
	 * Update or create the CHANGELOG.md file.
	 * <p>
	 * If the file already exists, trim the first header before prepending it with the new changelog entries.
	 *
	 * @param changelog The new changelog content
	 * @throws ChangelogException Thrown if file operations on the existing or new file are unsuccessful
	 */
	private void writeChangelog(String changelog) throws ChangelogException {
		Path changelogFile = this.baseDir.resolve(CHANGELOG_FILE);

		if (Files.exists(changelogFile)) {
			changelog = prependToExistingChangelog(changelogFile, changelog);
		}

		try {
			Files.writeString(changelogFile, changelog, TRUNCATE_EXISTING, CREATE);

		} catch (IOException e) {
			throw new ChangelogException("Failed to write " + changelogFile, e);
		}
	}

	/**
	 * @param changelogFile The current CHANGELOG.md file
	 * @param changelog     The new content to put before the existing content
	 * @return The final changelog content with both old and new content
	 * @throws ChangelogException If the existing changelog could not be read
	 */
	private static String prependToExistingChangelog(Path changelogFile, String changelog) throws ChangelogException {
		String existingChangelog;
		try {
			existingChangelog = Files.readString(changelogFile);
		} catch (IOException e) {
			throw new ChangelogException("Failed to read existing changelog" + changelogFile, e);
		}

		// Remove the header from the existing changelog, so the new one can be added before it
		existingChangelog = existingChangelog.stripLeading()
			.replaceFirst("# .*\n", "")
			.stripLeading();

		changelog = changelog + "\n" + existingChangelog;

		return changelog;
	}
}
