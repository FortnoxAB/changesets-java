package se.fortnox.changesets.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.fortnox.changesets.Changeset;
import se.fortnox.changesets.ChangesetWriter;
import se.fortnox.changesets.Level;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;

@Mojo(name = "add", defaultPhase = LifecyclePhase.INITIALIZE, aggregator = true)
public class AddMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private org.apache.maven.project.MavenProject project;

	/**
	 * Default content of the changeset.
	 */
	@Parameter(property = "changesetContent")
	String changesetContent;

	/**
	 * Level of the changeset, e.g. patch, minor, major.
	 */
	@Parameter(property = "changesetLevel", defaultValue = "patch")
	String changesetLevel;

	/**
	 * Create the changeset with a specific filename.
	 * By default, a filename will be automatically generated.
	 */
	@Parameter(property = "changesetFilename")
	String changesetFilename;

	@Override
	public void execute() {
		Path baseDir = project.getBasedir().toPath();

		ChangesetWriter changesetWriter = new ChangesetWriter(baseDir);

		Level level;
		try {
			level = Level.valueOf(this.changesetLevel.toUpperCase());
		} catch (IllegalArgumentException e) {
			List<String> validLevels = Arrays.stream(Level.values()).map(Enum::name).toList();
			getLog().error("Invalid changeset level: %s. Valid values are: %s".formatted(this.changesetLevel, validLevels));
			return;
		}

		File changesetFile = null;
		if (changesetFilename != null && !changesetFilename.isBlank()) {
			changesetFile = baseDir.resolve(CHANGESET_DIR).resolve(changesetFilename).toFile();
		}

		try {
			var changeset = new Changeset("%s".formatted(project.getArtifactId()), level, changesetContent, changesetFile);
			changesetWriter.writeChangeset(changeset);
		} catch (FileAlreadyExistsException e) {
			throw new RuntimeException(e);
		}
	}
}
