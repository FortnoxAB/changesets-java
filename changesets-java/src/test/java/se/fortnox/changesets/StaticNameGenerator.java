package se.fortnox.changesets;

public class StaticNameGenerator implements NameGenerator {
	private String name = "static-name";

	public StaticNameGenerator() {
	}

	public StaticNameGenerator(String name) {
		this.name = name;
	}

	@Override
	public String humanId() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
