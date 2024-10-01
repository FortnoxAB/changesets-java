package se.fortnox.changesets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RandomNameGeneratorTest {

	@Test
	void shouldGenerateRandomName() {
		String randomName = new RandomNameGenerator().humanId();

		assertThat(randomName).isNotEmpty();
		assertThat(randomName.split("-")).hasSize(3);
	}
}