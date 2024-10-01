package se.fortnox.changesets;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Basic reimplementation of <a href="https://github.com/RienNeVaPlus/human-id">human-id</a> to stay similar to
 * the original <a href="https://github.com/changesets/changesets">changesets</a> implementation.
 *
 */
public class RandomNameGenerator implements NameGenerator {
	private final String[] adjectives;
	private final String[] nouns;
	private final String[] verbs;
	private final Random random = new Random();

	public RandomNameGenerator() {
		ClassLoader classLoader = RandomNameGenerator.class.getClassLoader();
		this.adjectives = getStrings(classLoader, "adjectives.txt");
		this.nouns = getStrings(classLoader, "nouns.txt");
		this.verbs = getStrings(classLoader, "verbs.txt");
	}

	private String[] getStrings(ClassLoader classLoader, String file) {
		final String[] words;
		try (InputStream data = classLoader.getResourceAsStream(file)) {
			words = new BufferedReader(new InputStreamReader(data, StandardCharsets.UTF_8)).lines().toArray(String[]::new);
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}
		return words;
	}

	@Override
	public String humanId() {
		String adjective = getRandomWord(adjectives);
		String noun = getRandomWord(nouns);
		String verb = getRandomWord(verbs);

		return adjective + "-" + noun + "-" + verb;
	}

	private String getRandomWord(String[] words) {
		return words[random.nextInt(words.length)];
	}
}
