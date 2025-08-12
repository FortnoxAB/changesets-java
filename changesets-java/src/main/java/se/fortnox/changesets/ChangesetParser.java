package se.fortnox.changesets;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public class ChangesetParser {
	private static final Logger LOG = getLogger(ChangesetParser.class);

	public static List<Changeset> parseFile(File file) {
		String content;
		try {
			content = Files.readString(file.toPath());
		} catch (IOException e) {
			LOG.warn("Could not read {}", file, e);
			return new ArrayList<>();
		}

		Map<String, Object> frontMatter = FrontMatterParser.parse(content);

		Set<String> packages = frontMatter.keySet();

		return packages.stream()
			.filter(packageName -> packageName != null && !packageName.isEmpty())
			.map(packageName -> {
				String levelString = (String) frontMatter.get(packageName);

				// Translate to enum
				Level level = switch (levelString) {
					case "dependency" -> Level.DEPENDENCY;
					case "patch" -> Level.PATCH;
					case "minor" -> Level.MINOR;
					case "major" -> Level.MAJOR;
					default -> null;
				};

				if (level == null) {
					LOG.warn("Unexpected change level found in {}: \"{}\"", file, levelString);
					return Optional.<Changeset>empty();
				}

				String message = FrontMatterParser.removeFrontMatter(content).trim();
				return Optional.of(new Changeset(packageName, level, message, file));
			})
			.filter(Optional::isPresent)
			.map(Optional::get)
			.toList();
	}
}
