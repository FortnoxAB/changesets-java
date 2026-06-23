package se.fortnox.changesets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public record ChangesetsConfig(
	VersioningStrategy versioning,
	List<List<String>> linked,
	List<List<String>> fixed,
	ChangelogMode changelog
) {
	private static final Logger LOG = getLogger(ChangesetsConfig.class);
	public static final String CONFIG_FILE = "config.json";

	public ChangesetsConfig {
		versioning = versioning == null ? VersioningStrategy.FIXED : versioning;
		linked = linked == null ? List.of() : List.copyOf(linked);
		fixed = fixed == null ? List.of() : List.copyOf(fixed);
		changelog = changelog == null ? ChangelogMode.ROOT : changelog;
		validateGroupsAreDisjoint(linked, fixed);
	}

	public static ChangesetsConfig defaults() {
		return new ChangesetsConfig(VersioningStrategy.FIXED, List.of(), List.of(), ChangelogMode.ROOT);
	}

	/**
	 * Read .changeset/config.json from the given changesets directory.
	 * Returns defaults if the file does not exist or cannot be parsed.
	 */
	public static ChangesetsConfig load(Path changesetsDir) {
		Path configFile = changesetsDir.resolve(CONFIG_FILE);
		if (!Files.exists(configFile)) {
			return defaults();
		}
		try {
			String json = Files.readString(configFile);
			return new ObjectMapper().readValue(json, ChangesetsConfig.class);
		} catch (IOException e) {
			LOG.error("Failed to read changesets config at {}, falling back to defaults", configFile, e);
			return defaults();
		}
	}

	private static void validateGroupsAreDisjoint(List<List<String>> linked, List<List<String>> fixed) {
		Set<String> seen = new HashSet<>();
		List<List<String>> allGroups = new ArrayList<>(linked.size() + fixed.size());
		allGroups.addAll(linked);
		allGroups.addAll(fixed);
		for (List<String> group : allGroups) {
			for (String name : group) {
				if (!seen.add(name)) {
					throw new IllegalArgumentException(
						"Module '" + name + "' appears in multiple linked/fixed groups");
				}
			}
		}
	}

	public enum VersioningStrategy {
		@JsonProperty("fixed") FIXED,
		@JsonProperty("independent") INDEPENDENT;

		@JsonCreator
		public static VersioningStrategy fromString(String value) {
			if (value == null) {
				return FIXED;
			}
			return switch (value.toLowerCase()) {
				case "fixed" -> FIXED;
				case "independent" -> INDEPENDENT;
				default -> throw new IllegalArgumentException("Unknown versioning strategy: " + value);
			};
		}
	}

	public enum ChangelogMode {
		@JsonProperty("root") ROOT;

		@JsonCreator
		public static ChangelogMode fromString(String value) {
			if (value == null) {
				return ROOT;
			}
			return switch (value.toLowerCase()) {
				case "root" -> ROOT;
				default -> throw new IllegalArgumentException("Unknown changelog mode: " + value);
			};
		}
	}
}
