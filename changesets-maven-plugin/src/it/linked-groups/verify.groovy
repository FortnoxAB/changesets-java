import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("module-a=1.1.0")
// Linked: module-b has no changeset, so it MUST NOT bump
assertThat(versions).doesNotContain("module-b=")

def moduleA = new XmlSlurper().parse(new File(basedir, 'module-a/pom.xml'))
assertThat(moduleA.version).isEqualTo('1.1.1-SNAPSHOT')

def moduleB = new XmlSlurper().parse(new File(basedir, 'module-b/pom.xml'))
assertThat(moduleB.version).isEqualTo('1.0.0')

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

true
