import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

// changesets:prepare wrote VERSIONS but did NOT touch any poms (useReleasePluginIntegration=true).
//   module-a 2.0.5-SNAPSHOT + minor -> release 2.1.0 (minor escalates from non-boundary)
//   module-b 3.0.5-SNAPSHOT + patch -> release 3.0.5 (patch confirms the intended SNAPSHOT)
def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("module-a=2.1.0")
assertThat(versions).contains("module-b=3.0.5")
assertThat(versions).doesNotContain("rp-independent-root=")

// release:update-versions then consulted ChangesetsVersionPolicy per module, which resolved
// the next development version from VERSIONS keyed by artifactId.
def moduleA = new XmlSlurper().parse(new File(basedir, 'module-a/pom.xml'))
assertThat(moduleA.version).isEqualTo('2.1.1-SNAPSHOT')

def moduleB = new XmlSlurper().parse(new File(basedir, 'module-b/pom.xml'))
assertThat(moduleB.version).isEqualTo('3.0.6-SNAPSHOT')

// Root has no VERSIONS entry, so its version is unchanged.
def rootProject = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(rootProject.version).isEqualTo('1.0.0-SNAPSHOT')

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

def buildLog = new File(basedir, "build.log").text
assert buildLog =~ /Changesets processed, but not updating POMs due to useReleasePluginIntegration being set to true/

true
