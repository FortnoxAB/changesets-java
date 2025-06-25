package se.fortnox.changesets;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyUpdatesParserTest {

	@Test
	void shouldParseDependencyUpdates() {
		DependencyUpdatesParser parser = new DependencyUpdatesParser();
		List<String> dependencies = parser.parseDependencyChangeset("- maven:3.9");

		assertThat(dependencies)
			.containsExactly("maven:3.9");
	}

	@Test
	void shouldReturnUniqueDependencies() {
		DependencyUpdatesParser parser = new DependencyUpdatesParser();
		List<String> dependencies = parser.parseDependencyChangeset("""
		- org.apache.logging.log4j:log4j-core: 2.24.1
		- org.apache.logging.log4j:log4j-core: 2.24.2
		- org.apache.logging.log4j:log4j-core: 2.24.3
		- org.apache.logging.log4j:log4j-core: 2.24.3
		
		""");

		Set<String> expected = Set.of(
			"org.apache.logging.log4j:log4j-core: 2.24.1",
			"org.apache.logging.log4j:log4j-core: 2.24.2",
			"org.apache.logging.log4j:log4j-core: 2.24.3"
		);

		assertThat(dependencies)
			.containsExactlyInAnyOrderElementsOf(expected);
	}

}