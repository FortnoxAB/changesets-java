package se.fortnox.changesets;

public enum Level {
	MAJOR("major", "Major Changes"),
	MINOR("minor", "Minor Changes"),
	PATCH("patch", "Patch Changes"),
	DEPENDENCY("dependency", "Dependency Updates");

	private final String textValue;
	private final String presentationString;

	Level(String textValue, String presentationString) {
		this.textValue = textValue;
		this.presentationString = presentationString;
	}

	public String getTextValue() {
		return textValue;
	}

	public String getPresentationString() {
		return presentationString;
	}
}
