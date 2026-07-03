package se.fortnox.changesets.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Walks a BOM module's raw {@code <dependencyManagement>} to map managed reactor
 * artifacts to the property name that pins their version. Used to rewrite the
 * BOM's {@code <properties>} when sibling modules bump.
 */
public class BomResolver {
	private static final Pattern PROPERTY_REF = Pattern.compile("\\$\\{([^}]+)}");

	/**
	 * @param bom          The BOM Maven project (its original model is consulted).
	 * @param reactorIds   The set of reactor module {@code groupId:artifactId} keys.
	 * @return Ordered map of reactor artifactId → property name in the BOM that pins it.
	 *         Only entries whose {@code <version>} is a property reference are included.
	 */
	public static Map<String, String> resolvePinnedProperties(MavenProject bom, Map<String, String> reactorIds) {
		Map<String, String> result = new LinkedHashMap<>();
		Model model = bom.getOriginalModel();
		if (model == null) {
			return result;
		}
		DependencyManagement dm = model.getDependencyManagement();
		if (dm == null) {
			return result;
		}
		for (Dependency dep : dm.getDependencies()) {
			String key = dep.getGroupId() + ":" + dep.getArtifactId();
			String reactorArtifactId = reactorIds.get(key);
			if (reactorArtifactId == null) {
				continue;
			}
			String version = dep.getVersion();
			if (version == null) {
				continue;
			}
			Matcher m = PROPERTY_REF.matcher(version);
			if (m.matches()) {
				result.put(reactorArtifactId, m.group(1));
			}
		}
		return result;
	}
}
