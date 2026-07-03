import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

String expectedVersion = '2.0.0'
String expectedSnapshot = '2.0.1-SNAPSHOT'

// Default versioning (fixed): all reactor modules appear in VERSIONS at the same version
def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("multi-module=${expectedVersion}")
assertThat(versions).contains("module1=${expectedVersion}")
assertThat(versions).contains("module2=${expectedVersion}")

// Root pom version is bumped to next-dev SNAPSHOT
def project = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(project.version).isEqualTo(expectedSnapshot)

// Submodule parent refs are synced to the root SNAPSHOT
def submodule1 = new XmlSlurper().parse(new File(basedir, 'module1/pom.xml'))
assertThat(submodule1.parent.version).isEqualTo(project.version)

def submodule2 = new XmlSlurper().parse(new File(basedir, 'module2/pom.xml'))
assertThat(submodule2.parent.version).isEqualTo(project.version)

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

true
