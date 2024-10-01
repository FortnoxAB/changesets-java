package se.fortnox.changesets;

import java.io.File;

public record Changeset(String packageName, Level level, String message, File file) {

	public static Changeset blank(String packageName) {
		return new Changeset(packageName, Level.PATCH, null, null);
	}
}
