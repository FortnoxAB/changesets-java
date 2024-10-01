package se.fortnox.changesets.maven;

import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.stax2.XMLInputFactory2;
import org.slf4j.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Some of this stuff is blatantly copy/pasted from versions-maven-plugin, so that we can reuse some of their code for updating the pom files.
 *
 * TODO Needs tests
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
	 * @param newVersion The new version to set the project parent reference to to
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

	private static void updatePom(File outFile, Consumer<ModifiedPomXMLEventReader> updater) {
		try {
			StringBuilder   input        = PomHelper.readXmlFile(outFile);
			XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
			inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
			ModifiedPomXMLEventReader newPom = new ModifiedPomXMLEventReader(input, inputFactory, outFile.getAbsolutePath());

			updater.accept(newPom);

			try (Writer writer = WriterFactory.newXmlWriter(outFile)) {
				IOUtil.copy(input.toString(), writer);
			}
		} catch (XMLStreamException | IOException e) {
			LOG.error("Failed to update {}", outFile, e);
		}
	}
}
