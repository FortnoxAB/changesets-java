package se.fortnox.changesets;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static ch.qos.logback.classic.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;

class ChangesetParserTest {
	private static final Path BASE_DIR = Path.of("src/test/resources/changesets/");

	private ListAppender<ILoggingEvent> appender;
	private Logger                      logger = (Logger) LoggerFactory.getLogger(ChangesetParser.class);

	@BeforeEach
	public void setUp() {
		appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
	}

	@AfterEach
	public void tearDown() {
		logger.detachAppender(appender);
	}

	@ParameterizedTest
	@ValueSource(strings = {"patch", "minor", "major"})
	void shouldSetLevelFromFile(String level) {
		File changesetFile = getTestFile("level-" + level + ".md");
		List<Changeset> changesets = ChangesetParser.parseFile(changesetFile);

		assertThat(changesets)
			.hasSize(1)
			.first()
			.satisfies(changeset -> {
				Level expectedLevel = Level.valueOf(level.toUpperCase());
				assertThat(changeset.level()).isEqualTo(expectedLevel);
			});
	}

	@Test
	void shouldParseFile() {
		File changesetFile = getTestFile("mixed-levels/.changeset/eight-owls-watch.md");

		List<Changeset> changesets = ChangesetParser.parseFile(changesetFile);

		assertThat(changesets)
			.hasSize(1)
			.first()
			.satisfies(changeset -> {
				assertThat(changeset.file()).isEqualTo(changesetFile);
				assertThat(changeset.level()).isEqualTo(Level.MAJOR);
				assertThat(changeset.packageName()).isEqualTo("my-package");
				assertThat(changeset.message()).isEqualTo("A great change");
			});
	}

	@Test
	void shouldReturnOneChangesetPerPackage() {
		File            changesetFile = getTestFile("changeset-with-multiple-packages.md");
		List<Changeset> changesets = ChangesetParser.parseFile(changesetFile);

		assertThat(changesets)
			.hasSize(2)
			.allSatisfy(changeset -> {
				assertThat(changeset.file()).isEqualTo(changesetFile);
				assertThat(changeset.message()).isEqualTo("A test change");
			})
			.satisfiesOnlyOnce(changeset -> {
				assertThat(changeset.level()).isEqualTo(Level.MAJOR);
				assertThat(changeset.packageName()).isEqualTo("some-other-package");
			})
			.satisfiesOnlyOnce(changeset -> {
				assertThat(changeset.level()).isEqualTo(Level.PATCH);
				assertThat(changeset.packageName()).isEqualTo("my-package");
			});
	}

	private static File getTestFile(String other) {
		File changesetFile = BASE_DIR.resolve(other).toFile();
		assertThat(changesetFile).exists();
		return changesetFile;
	}

	@Test
	void shouldSkipChangesetWithInvalidLevel() {
		File changesetFile = getTestFile("changeset-with-invalid-level.md");

		List<Changeset> changesets = ChangesetParser.parseFile(changesetFile);

		assertThat(changesets).isEmpty();

		assertThat(appender.list)
			.anySatisfy(loggingEvent -> {
				assertThat(loggingEvent.getLevel()).isEqualTo(WARN);
				assertThat(loggingEvent.getFormattedMessage())
					.isEqualTo("Unexpected change level found in src/test/resources/changesets/changeset-with-invalid-level.md: \"error\"");
			});
	}

	@Test
	void shouldSkipChangesetWithEmptyLevel() {
		File changesetFile = getTestFile("changeset-with-empty-level.md");

		List<Changeset> changesets = ChangesetParser.parseFile(changesetFile);

		assertThat(changesets).isEmpty();

		assertThat(appender.list)
			.anySatisfy(loggingEvent -> {
				assertThat(loggingEvent.getLevel()).isEqualTo(WARN);
				assertThat(loggingEvent.getFormattedMessage())
					.isEqualTo("Unexpected change level found in src/test/resources/changesets/changeset-with-empty-level.md: \"\"");
			});
	}

	@Test
	void shouldSkipFilesWithoutFrontMatter() {
		File changesetFile = getTestFile("changeset-with-no-frontmatter.md");

		List<Changeset> changesets = ChangesetParser.parseFile(changesetFile);

		assertThat(changesets).isEmpty();
	}

	@Test
	void shouldWarnIfFileIsNotFound() {
		File changesetFile = Path.of("non-existing-file.md").toFile();
		assertThat(changesetFile).doesNotExist();

		List<Changeset> changesets = ChangesetParser.parseFile(changesetFile);

		assertThat(changesets).isEmpty();

		assertThat(appender.list)
			.anySatisfy(loggingEvent -> {
				assertThat(loggingEvent.getLevel()).isEqualTo(WARN);
				assertThat(loggingEvent.getFormattedMessage())
					.isEqualTo("Could not read non-existing-file.md");
			});
	}
}