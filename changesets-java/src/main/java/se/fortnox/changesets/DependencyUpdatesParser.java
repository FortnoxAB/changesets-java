package se.fortnox.changesets;

import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.collection.iteration.ReversiblePeekingIterable;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.slf4j.LoggerFactory.getLogger;

public class DependencyUpdatesParser {
	private static final Logger LOG = getLogger(DependencyUpdatesParser.class);

	public List<String> parseDependencyChangeset(String change) {
		Parser parser = Parser.builder().build();
		Document parsed = parser.parse(change);

		if (!parsed.hasChildren()) {
			return List.of();
		}

		String nodeName = parsed.getFirstChild().getNodeName();
		if (!nodeName.equals("BulletList")) {
			LOG.warn("Unexpected node type {}", nodeName);
			System.out.println();
			return List.of();
		}

		ReversiblePeekingIterable<Node> dependencyNodes = parsed.getFirstChild().getChildren();

		return StreamSupport.stream(dependencyNodes.spliterator(), false)
			.map(node -> {
				Paragraph paragraph = (Paragraph)node.getChildOfType(Paragraph.class);
				return paragraph.getChars().toString().trim();
			})
			.distinct()
			.toList();
	}
}
