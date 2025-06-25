package se.fortnox.changesets;

import io.hosuaby.inject.resources.junit.jupiter.GivenTextResource;
import io.hosuaby.inject.resources.junit.jupiter.TestWithResources;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@TestWithResources
class MarkdownFormatterTest {
    @GivenTextResource("markdown-tests/formatter.original.md")
    String original;

    @GivenTextResource("markdown-tests/formatter.expected.md")
    String expected;

    @Test
    void shouldFormatDocumentAsExpected() {
	    String actual = MarkdownFormatter.format(original);
	    assertThat(actual).isEqualTo(expected);
    }

	/**
	 * The double spaces at the end indicate that the following line should be indented as the current one and must be kept
	 */
	@Test
	void shouldKeepMultilineBulletItems() {
		String unformatted = """
			- First line\s\s
			  Second line\s\s
			  Third line
			
			""";

		String actual = MarkdownFormatter.format(unformatted);
		assertThat(actual).isEqualTo(unformatted);
	}
}