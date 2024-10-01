package se.fortnox.changesets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

public class FrontMatterParser {
	private static final Logger LOG = getLogger(FrontMatterParser.class);

	/**
	 * Parse front matter in a Markdown document and return it as a Map.
	 *
	 * @param document Markdown document
	 * @return Front matter values
	 */
	public static Map<String, Object> parse(String document) {
		Pattern pattern = Pattern.compile("^\\s*-{3}([\\s\\S]*)-{3}");
		Matcher matcher = pattern.matcher(document);

		if (!matcher.find()) {
			return new HashMap<>();
		}
		String frontmatterString = matcher.group(1);

		YAMLMapper mapper = new YAMLMapper();
		Map<String, Object> mapped = new HashMap<>();
		try {
			mapped = mapper.readValue(frontmatterString, new TypeReference<HashMap<String,Object>>(){});

		} catch (JsonMappingException exception) {
			LOG.error("Failed to parse front matter", exception);
			return mapped;

		} catch (JsonProcessingException exception) {
			throw new RuntimeException(exception);
		}

		return mapped;
	}

	/**
	 * Strip any front matter from the passed document and trim leading white space.
	 *
	 * @param document Markdown document
	 * @return The same Markdown document with front matter removed
	 */
	public static String removeFrontMatter(String document) {
		Pattern pattern = Pattern.compile("^\\s*-{3}([\\s\\S]*)(-{3})");
		Matcher matcher = pattern.matcher(document);

		if (!matcher.find()) {
			return document.stripLeading();
		}

		int endIndex = matcher.end(2);
		return document.substring(endIndex).stripLeading();
	}
}
