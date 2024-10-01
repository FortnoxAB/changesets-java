package se.fortnox.changesets;

public enum Level {
	MAJOR("major", "Major"),
	MINOR("minor", "Minor"),
	PATCH("patch", "Patch");

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
