import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

// Both members have changesets at different levels; both bump to the highest-level result
// of the highest current version in the linked set. From [1.0.0, 1.0.0] + minor = 1.1.0.
def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("module-a=1.1.0")
assertThat(versions).contains("module-b=1.1.0")

def moduleA = new XmlSlurper().parse(new File(basedir, 'module-a/pom.xml'))
assertThat(moduleA.version).isEqualTo('1.1.1-SNAPSHOT')

def moduleB = new XmlSlurper().parse(new File(basedir, 'module-b/pom.xml'))
assertThat(moduleB.version).isEqualTo('1.1.1-SNAPSHOT')

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

true
