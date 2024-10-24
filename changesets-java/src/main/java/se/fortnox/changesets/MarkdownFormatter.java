package se.fortnox.changesets;

import com.vladsch.flexmark.formatter.Formatter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.LineAppendable;

import static com.vladsch.flexmark.formatter.Formatter.FORMAT_FLAGS;
import static com.vladsch.flexmark.formatter.Formatter.RIGHT_MARGIN;
import static com.vladsch.flexmark.util.data.SharedDataKeys.BLANK_LINES_IN_AST;

public class MarkdownFormatter {
    /**
     * Format a Markdown document for consistency.
     *
     * @param markdown The unformatted Markdown document
     * @return The formatted Markdown document
     */
	public static String format(String markdown) {
		MutableDataSet formatOptions = new MutableDataSet();
		// Clean up whitespaces in different ways
		formatOptions.set(FORMAT_FLAGS, LineAppendable.F_FORMAT_ALL);

        // Limit line lengths
		formatOptions.set(RIGHT_MARGIN, 120);

        // Leaving this on false adds new lines in strange places, unsure why
		formatOptions.set(BLANK_LINES_IN_AST, true);

		Formatter formatter = Formatter.builder(formatOptions).build();
		Parser parser = Parser.builder().build();
		Document parsed = parser.parse(markdown);

        return formatter.render(parsed);
	}
}
