package se.fortnox.changesets.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.fortnox.changesets.Changeset;
import se.fortnox.changesets.ChangesetWriter;
import se.fortnox.changesets.Level;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;

@Mojo(name = "add", defaultPhase = LifecyclePhase.INITIALIZE, aggregator = true)
public class AddMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private org.apache.maven.project.MavenProject project;

	@Parameter(property = "changesetContent")
	String changesetContent;

	@Override
	public void execute() {
		Path baseDir = project.getBasedir().toPath();

		ChangesetWriter changesetWriter = new ChangesetWriter(baseDir);

		try {
			var changeset = new Changeset("%s:%s".formatted(project.getGroupId(), project.getArtifactId()), Level.PATCH, changesetContent, null);
			changesetWriter.writeChangeset(changeset);
		} catch (FileAlreadyExistsException e) {
			throw new RuntimeException(e);
		}
	}
}
