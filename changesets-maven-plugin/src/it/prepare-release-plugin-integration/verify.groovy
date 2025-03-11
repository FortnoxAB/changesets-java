import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

String expectedVersion = '2.5.0';
String expectedSnapshot = '2.5.1-SNAPSHOT';

// The VERSION file should contain the correct version number
assertThat(new File(basedir, '.changeset/VERSION'))
        .content()
        .isEqualTo(expectedVersion)

// The root pom version should be increased by one patch and be a snapshot
def project = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(project.version).isEqualTo(expectedSnapshot)

// Verify that the CHANGELOG.md has been created correctly
assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

def buildLog = new File( basedir, "build.log").text
assert buildLog =~ /Changesets processed, but not updating POMs due to useReleasePluginIntegration being set to true/
true