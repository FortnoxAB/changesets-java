import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

String expectedVersion = '2.5.0';

// The VERSION file should contain the correct version number
assertThat(new File(basedir, '.changeset/VERSION'))
        .content()
        .isEqualTo(expectedVersion)

// The root pom version should have the same value as the VERSION file
def project = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(project.version).isEqualTo(expectedVersion)

// Check that the parent reference is updated to the new version
def submodule1 = new XmlSlurper().parse(new File(basedir, 'module1/pom.xml'))
assertThat(submodule1.parent.version).isEqualTo(project.version)


// Check that the parent reference is updated to the new version
def submodule2 = new XmlSlurper().parse(new File(basedir, 'module2/pom.xml'))
assertThat(submodule2.parent.version).isEqualTo(project.version)

true