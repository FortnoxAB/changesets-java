package se.fortnox.changesets;

import com.adelean.inject.resources.junit.jupiter.GivenTextResource;
import com.adelean.inject.resources.junit.jupiter.TestWithResources;
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
        assertThat(MarkdownFormatter.format(original)).isEqualTo(expected);
    }

}