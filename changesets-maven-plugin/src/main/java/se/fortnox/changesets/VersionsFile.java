package se.fortnox.changesets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

import static se.fortnox.changesets.ChangesetWriter.CHANGESET_DIR;

/**
 * Read/write helper for {@code .changeset/VERSIONS} — the prepare→release handoff file
 * keyed by artifactId. Only modules that bumped in a release are present.
 */
public class VersionsFile {
	public static final String FILE = "VERSIONS";

	public static Path locate(Path reactorRoot) {
		return reactorRoot.resolve(CHANGESET_DIR).resolve(FILE);
	}

	public static Map<String, String> read(Path reactorRoot) {
		Path file = locate(reactorRoot);
		if (!Files.exists(file)) {
			return Map.of();
		}
		Properties props = new Properties();
		try (BufferedReader r = Files.newBufferedReader(file)) {
			props.load(r);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read " + file, e);
		}
		Map<String, String> out = new LinkedHashMap<>();
		for (String key : props.stringPropertyNames()) {
			out.put(key, props.getProperty(key));
		}
		return out;
	}

	public static Optional<String> lookup(Path reactorRoot, String artifactId) {
		return Optional.ofNullable(read(reactorRoot).get(artifactId));
	}

	public static void write(Path reactorRoot, Map<String, String> versions) {
		Path file = locate(reactorRoot);
		try {
			Files.createDirectories(file.getParent());
			try (BufferedWriter w = Files.newBufferedWriter(file)) {
				for (Map.Entry<String, String> e : new TreeMap<>(versions).entrySet()) {
					w.write(e.getKey());
					w.write('=');
					w.write(e.getValue());
					w.newLine();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to write " + file, e);
		}
	}
}
