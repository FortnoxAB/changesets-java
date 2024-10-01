package se.fortnox.changesets;

import org.semver4j.Semver;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class VersionCalculator {

	public static String getNewVersion(String version, List<Changeset> changes) {
		Semver semanticVersion = Optional.ofNullable(Semver.coerce(version))
			.map(Semver::withClearedBuild)
			.orElseThrow(() -> new IllegalArgumentException("Cannot coerce \"%s\" into a semantic version.".formatted(version)));

		Set<Level> levels = changes.stream()
			.map(Changeset::level)
			.collect(Collectors.toSet());

		if (levels.contains(Level.MAJOR)) {
			return semanticVersion.nextMajor().getVersion();
		} else if (levels.contains(Level.MINOR)) {
			return semanticVersion.nextMinor().getVersion();
		} else if (levels.contains(Level.PATCH)) {
			return semanticVersion.nextPatch().getVersion();
		}
		return semanticVersion.getVersion();
	}
}
