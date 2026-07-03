import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

String expectedVersion = '2.5.0'
String expectedSnapshot = '2.5.1-SNAPSHOT'

assertThat(new File(basedir, '.changeset/VERSIONS'))
        .content()
        .isEqualToIgnoringNewLines("my-package=${expectedVersion}")

// release:update-versions sets the pom to the next development version derived from VERSIONS
def project = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(project.version).isEqualTo(expectedSnapshot)

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

def buildLog = new File(basedir, "build.log").text
assert buildLog =~ /Changesets processed, but not updating POMs due to useReleasePluginIntegration being set to true/
true
