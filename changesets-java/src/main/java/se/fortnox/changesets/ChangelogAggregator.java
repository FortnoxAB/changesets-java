package se.fortnox.changesets;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
import static se.fortnox.changesets.Level.DEPENDENCY;
import static se.fortnox.changesets.Level.MAJOR;
import static se.fortnox.changesets.Level.MINOR;
import static se.fortnox.changesets.Level.PATCH;

public class ChangelogAggregator {
	private static final Logger LOG = getLogger(ChangelogAggregator.class);
	public static final String CHANGELOG_FILE = "CHANGELOG.md";
	private final Path baseDir;
	private final DependencyUpdatesParser dependencyUpdatesParser;

	public ChangelogAggregator(Path baseDir) {
		this(baseDir, new DependencyUpdatesParser());
	}

	public ChangelogAggregator(Path baseDir, DependencyUpdatesParser dependencyUpdatesParser) {
		this.baseDir = baseDir;
		this.dependencyUpdatesParser = dependencyUpdatesParser;
	}

	/**
	 * Merge any changesets in baseDir matching the package name into CHANGELOG.md,
	 * creating it if it exists or prepending it to the existing content.
	 *
	 * @param packageName The package name to get changesets for
	 * @param version     The version number of the merged changes
	 * @return
	 */
	public Path mergeChangesetsToChangelog(String packageName, String version) {
		Path changesetsDir = this.baseDir.resolve(CHANGESET_DIR);

		ChangesetLocator changesetLocator = new ChangesetLocator(this.baseDir);
		List<Changeset> changesets = changesetLocator.getChangesets(packageName);
		if (changesets.isEmpty()) {
			LOG.info("No changesets found in {}", this.baseDir);
			return changesetsDir;
		}

		String changelog = generateChangelog(packageName, version, changesets);

		Path changelogFile;
		try {
			changelogFile = writeChangelog(changelog);
		} catch (ChangelogException exception) {
			LOG.error("Failed to update changelog at {}", changesetsDir, exception);
			return changesetsDir;
		}

		changesets.forEach(changeset -> {
			File file = changeset.file();
			try {
				Files.delete(file.toPath());
			} catch (IOException e) {
				LOG.error("Failed to delete {}", file, e);
			}
		});
		return changelogFile;
	}

	/**
	 * Merge a multi-module release into the root CHANGELOG.md: one section per module that
	 * bumped, ordered by the {@code moduleEntries} iteration order. Consumes (deletes) all
	 * passed-in changeset files. Modules with empty changeset lists are skipped.
	 *
	 * @param moduleEntries Ordered map of artifactId → (newVersion, changesets) for the release
	 * @return Path to the written CHANGELOG.md, or the changeset dir if nothing was written
	 */
	public Path mergeReleaseToChangelog(Map<String, ReleaseEntry> moduleEntries) {
		return mergeReleaseToChangelog(moduleEntries, null);
	}

	/**
	 * BOM-aware variant. When {@code bomContext} is non-null, emits a single top-level
	 * release header (consumer-parent + BOM version) with each bumped module rendered
	 * as a nested sub-section. The BOM's section additionally lists its pinned-version
	 * updates.
	 */
	public Path mergeReleaseToChangelog(Map<String, ReleaseEntry> moduleEntries, BomContext bomContext) {
		Path changesetsDir = this.baseDir.resolve(CHANGESET_DIR);

		List<ReleaseEntry> renderable = moduleEntries.values().stream()
			.filter(e -> !e.changesets().isEmpty()
				|| (bomContext != null && e.artifactId().equals(bomContext.bomArtifactId())))
			.toList();
		if (renderable.isEmpty()) {
			LOG.info("No changesets to write to changelog in {}", this.baseDir);
			return changesetsDir;
		}

		String changelog = bomContext == null
			? generateMultiModuleChangelog(moduleEntries)
			: generateBomChangelog(moduleEntries, bomContext);

		Path changelogFile;
		try {
			changelogFile = writeChangelog(changelog);
		} catch (ChangelogException exception) {
			LOG.error("Failed to update changelog at {}", changesetsDir, exception);
			return changesetsDir;
		}

		deleteConsumedChangesets(moduleEntries.values().stream()
			.filter(e -> !e.changesets().isEmpty())
			.toList());
		return changelogFile;
	}

	public record ReleaseEntry(String artifactId, String newVersion, List<Changeset> changesets) {}

	/**
	 * Context for BOM-aware changelog rendering.
	 *
	 * @param headerArtifactId The artifactId shown in the top-level {@code ##} release header
	 *                         (consumer-parent if configured, otherwise the BOM itself).
	 * @param headerVersion    The version shown in the top-level header (the BOM's version).
	 * @param bomArtifactId    The BOM's artifactId — its section gets the pinned-versions block.
	 * @param pinnedUpdates    Ordered map of pinned module artifactId → new version, as
	 *                         applied to the BOM's {@code <properties>}.
	 */
	public record BomContext(
		String headerArtifactId,
		String headerVersion,
		String bomArtifactId,
		Map<String, String> pinnedUpdates
	) {}

	private void deleteConsumedChangesets(List<ReleaseEntry> entries) {
		entries.stream()
			.flatMap(e -> e.changesets().stream())
			.map(Changeset::file)
			.filter(java.util.Objects::nonNull)
			.distinct()
			.forEach(file -> {
				try {
					Files.deleteIfExists(file.toPath());
				} catch (IOException e) {
					LOG.error("Failed to delete {}", file, e);
				}
			});
	}

	private String generateMultiModuleChangelog(Map<String, ReleaseEntry> moduleEntries) {
		String body = moduleEntries.values().stream()
			.filter(e -> !e.changesets().isEmpty())
			.map(this::generateModuleSection)
			.collect(Collectors.joining("\n\n"));

		String markdown = """
			# Changelog

			%s
			""".formatted(body);

		return MarkdownFormatter.format(markdown);
	}

	private String generateModuleSection(ReleaseEntry entry) {
		String changes = renderChangesByLevel(entry.changesets(), "###");
		return """
			## %s@%s

			%s""".formatted(entry.artifactId(), entry.newVersion(), changes);
	}

	private String generateBomChangelog(Map<String, ReleaseEntry> moduleEntries, BomContext bom) {
		List<String> sections = new ArrayList<>();
		for (ReleaseEntry entry : moduleEntries.values()) {
			if (entry.artifactId().equals(bom.bomArtifactId())) {
				continue;
			}
			if (entry.changesets().isEmpty()) {
				continue;
			}
			sections.add(generateBomNestedSection(entry));
		}

		ReleaseEntry bomEntry = moduleEntries.get(bom.bomArtifactId());
		if (bomEntry != null) {
			sections.add(generateBomOwnSection(bomEntry, bom));
		}

		String body = String.join("\n\n", sections);

		String markdown = """
			# Changelog

			## %s@%s

			%s
			""".formatted(bom.headerArtifactId(), bom.headerVersion(), body);

		return MarkdownFormatter.format(markdown);
	}

	private String generateBomNestedSection(ReleaseEntry entry) {
		String changes = renderChangesByLevel(entry.changesets(), "####");
		return """
			### %s@%s

			%s""".formatted(entry.artifactId(), entry.newVersion(), changes);
	}

	private String generateBomOwnSection(ReleaseEntry bomEntry, BomContext bom) {
		StringBuilder section = new StringBuilder();
		section.append("### ").append(bomEntry.artifactId()).append('@').append(bomEntry.newVersion()).append("\n\n");

		if (!bomEntry.changesets().isEmpty()) {
			section.append(renderChangesByLevel(bomEntry.changesets(), "####")).append("\n\n");
		}

		if (!bom.pinnedUpdates().isEmpty()) {
			section.append("#### Pinned version updates\n\n");
			for (Map.Entry<String, String> e : bom.pinnedUpdates().entrySet()) {
				section.append("- ").append(e.getKey()).append('@').append(e.getValue()).append('\n');
			}
		}
		return section.toString();
	}

	private String generateChangelog(String packageName, String version, List<Changeset> changesets) {
		String changes = renderChangesByLevel(changesets, "###");

		String markdown = """
			# %s

			## %s

			%s
			""".formatted(packageName, version, changes);

		return MarkdownFormatter.format(markdown);
	}

	private String renderChangesByLevel(List<Changeset> changesets, String headingPrefix) {
		return changesets
			.stream()
			.collect(groupingBy(Changeset::level, mapping(Changeset::message, toList())))
			.entrySet()
			.stream()
			.sorted(sortChangesets())
			.map(entry -> {
				Level level = entry.getKey();
				String levelString = level.getPresentationString();
				String levelChanges = formatChangeset(level, entry.getValue());

				return """
					%s %s

					%s""".formatted(headingPrefix, levelString, levelChanges);
			})
			.collect(Collectors.joining("\n\n"));
	}

	private String formatChangeset(Level level, List<String> changes) {
		if (level == DEPENDENCY) {
			// Extract all dependencies from each dependency change and put them into a single list
			return changes.stream()
				.flatMap(change -> dependencyUpdatesParser.parseDependencyChangeset(change).stream())
				.map(ChangelogAggregator::formatChangeAsBulletPoint)
				.distinct()
				.sorted()
				.collect(Collectors.joining("\n"));
		}

		return changes.stream()
			.map(ChangelogAggregator::formatChangeAsBulletPoint)
			.sorted()
			.collect(Collectors.joining("\n"));
	}

	private static String formatChangeAsBulletPoint(String change) {
		// Add the change as a bullet point, with leading dash and each subsequent line indented with two spaces
		String firstLinePrefix = "- ";
		String eachLinePrefix = "  ";
		String lines = Arrays.stream(change.trim().split("\\R"))
			.map(line -> {
				// Do not indent empty lines
				if (line.isBlank()) {
					return line.trim();
				}
				return eachLinePrefix + line;
			})
			.collect(Collectors.joining("\n"))
			.substring(eachLinePrefix.length());

		return firstLinePrefix + lines;
	}

	private static Comparator<Map.Entry<Level, List<String>>> sortChangesets() {
		List<Level> levelOrder = List.of(MAJOR, MINOR, PATCH, DEPENDENCY);

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
	 * @return
	 * @throws ChangelogException Thrown if file operations on the existing or new file are unsuccessful
	 */
	private Path writeChangelog(String changelog) throws ChangelogException {
		Path changelogFile = this.baseDir.resolve(CHANGELOG_FILE);

		if (Files.exists(changelogFile)) {
			changelog = prependToExistingChangelog(changelogFile, changelog);
		}

		try {
			Files.writeString(changelogFile, changelog, TRUNCATE_EXISTING, CREATE);
			return changelogFile;

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
