package se.fortnox.changesets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FrontMatterParserTest {

	@Test
	void shouldParseBasicFrontMatter() {
		String document = """
		---
		data: hello
		---

		Body
		""";

		assertThat(FrontMatterParser.parse(document))
			.containsEntry("data", "hello");
	}

	@Test
	void shouldParseFrontMatterWithQuotedKey() {
		String document = """
		---
		"data-key": hello
		---

		Body
		""";

		assertThat(FrontMatterParser.parse(document))
			.containsEntry("data-key", "hello");
	}

	@Test
	void shouldHandleMissingFrontMatter() {
		String document = """
		Body
		""";

		assertThat(FrontMatterParser.parse(document))
			.isEmpty();

	}

	@Test
	void shouldIgnoreTripleDashesAfterFirst() {
		String document = """
		Body
		---
		not: frontmatter
		---
		other body
		""";

		assertThat(FrontMatterParser.parse(document))
			.isEmpty();

	}

	@Test
	void shouldRemoveFrontMatterFromDocument() {
		String document = """
		---
		"data-key": hello
		---

		Body
		""";

		String expectedTrimmedDocument = """
		Body
		""";

		assertThat(FrontMatterParser.removeFrontMatter(document))
			.isEqualTo(expectedTrimmedDocument);
	}

	@Test
	void shouldTrimLeadingWhitespacesEvenIfNoFrontMatterIsFound() {
		String document = """

		Body
		""";

		String expectedTrimmedDocument = """
		Body
		""";

		assertThat(FrontMatterParser.removeFrontMatter(document))
			.isEqualTo(expectedTrimmedDocument);
	}
}