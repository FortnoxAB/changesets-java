package se.fortnox.changesets.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;

@Mojo(name = "init", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private org.apache.maven.project.MavenProject project;

	public void execute() {
		Path baseDir = project.getBasedir().toPath();
		Path changesetDir = baseDir.resolve(CHANGESET_DIR);

		try {
			Files.createDirectory(changesetDir);
		} catch (IOException e) {
			getLog().error("Failed to create " + changesetDir, e);
		}

	}
}
