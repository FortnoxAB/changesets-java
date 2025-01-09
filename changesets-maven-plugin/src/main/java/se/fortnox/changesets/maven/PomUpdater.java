package se.fortnox.changesets.maven;

import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.rewriting.MutableXMLStreamReader;
import org.slf4j.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Some of this is copy/pasted from <a href="https://www.mojohaus.org/versions/versions-maven-plugin/index.html">versions-maven-plugin</a>,
 * so that we can reuse some of their code for updating the pom files.
 */
public class PomUpdater {
	private static final Logger LOG = getLogger(PomUpdater.class);

	/**
	 * Change project version in a pom file
	 *
	 * @param outFile    The pom file to update
	 * @param newVersion The new version to set the project to
	 */
	public static void setProjectVersion(File outFile, String newVersion) {
		updatePom(outFile, newPom -> {
			try {
				PomHelper.setProjectVersion(newPom, newVersion);
			} catch (XMLStreamException e) {
				LOG.error("Failed to update {}", outFile, e);
			}
		});

	}

	/**
	 * Set parent project version in a pom file
	 *
	 * @param outFile    The pom file to update
	 * @param newVersion The new version to set the project parent reference to
	 */
	public static void setProjectParentVersion(File outFile, String newVersion) {
		updatePom(outFile, newPom -> {
			try {
				PomHelper.setProjectParentVersion(newPom, newVersion);
			} catch (XMLStreamException e) {
				LOG.error("Failed to update {}", outFile, e);
			}
		});
	}

	private static void updatePom(File outFile, Consumer<MutableXMLStreamReader> updater) {
		try (MutableXMLStreamReader newPom = new MutableXMLStreamReader(outFile.toPath())) {
			updater.accept(newPom);
			if(newPom.isModified()) {
				try (Writer writer = Files.newBufferedWriter(
					outFile.toPath(),
					ofNullable(newPom.getEncoding()).map(Charset::forName).orElse(Charset.defaultCharset()))) {
					writer.write(newPom.getSource());
				}
			}
		} catch (XMLStreamException | IOException | TransformerException e) {
			LOG.error("Failed to update {}", outFile, e);
		}
	}
}
