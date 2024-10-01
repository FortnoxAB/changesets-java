package se.fortnox.changesets;

import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;
import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;

public class ChangesetLocator {
	private static final Logger LOG = getLogger(ChangesetLocator.class);
	private final Path baseDir;

	public ChangesetLocator(Path baseDir) {
		this.baseDir = baseDir;
	}

	public List<Changeset> getChangesets(String packageName) {
		Path changesetsDir = this.baseDir.resolve(CHANGESET_DIR);
		File[] array = changesetsDir.toFile().listFiles((dir, name) -> name.endsWith(".md"));

		if (array == null) {
			LOG.debug("No changesets found in {}", changesetsDir);
			return new ArrayList<>();
		}

		List<File> changesets = Arrays.stream(Objects.requireNonNull(array))
			.sorted()
			.toList();

		List<Changeset> matchingChangesets = changesets.stream()
			.flatMap(file -> ChangesetParser.parseFile(file).stream())
			.filter(changeset -> {
				boolean matchesPackage = changeset.packageName().equals(packageName);
				if (!matchesPackage) {
					LOG.info("Found {}, but {} did not match requested packagename {}", changeset.file(), changeset.packageName(), packageName);
				}

				return matchesPackage;
			})
			.toList();

		if (matchingChangesets.isEmpty()) {
			LOG.info("No changesets matching package {} found in {}", packageName, changesetsDir);
		}

		return matchingChangesets;
	}
}
