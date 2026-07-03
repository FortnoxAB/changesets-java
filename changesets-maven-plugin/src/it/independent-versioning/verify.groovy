import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("module-a=2.1.0")
assertThat(versions).contains("module-b=3.0.1")
// Root should NOT bump (no changeset targets it)
assertThat(versions).doesNotContain("independent-root=")

// Root version unchanged
def rootProject = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(rootProject.version).isEqualTo('1.0.0')

// Submodules each bumped to their own next-dev SNAPSHOT
def moduleA = new XmlSlurper().parse(new File(basedir, 'module-a/pom.xml'))
assertThat(moduleA.version).isEqualTo('2.1.1-SNAPSHOT')

def moduleB = new XmlSlurper().parse(new File(basedir, 'module-b/pom.xml'))
assertThat(moduleB.version).isEqualTo('3.0.2-SNAPSHOT')

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

true
