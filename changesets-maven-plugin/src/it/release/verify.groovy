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

true
