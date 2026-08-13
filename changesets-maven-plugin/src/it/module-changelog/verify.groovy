import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("module-a=2.1.0")
assertThat(versions).contains("module-b=3.0.1")

// Submodules each bumped to their own next-dev SNAPSHOT
def moduleA = new XmlSlurper().parse(new File(basedir, 'module-a/pom.xml'))
assertThat(moduleA.version).isEqualTo('2.1.1-SNAPSHOT')

def moduleB = new XmlSlurper().parse(new File(basedir, 'module-b/pom.xml'))
assertThat(moduleB.version).isEqualTo('3.0.2-SNAPSHOT')

// Per-module changelogs written next to each module's pom
assertThat(new File(basedir, 'module-a/CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'module-a/EXPECTED_CHANGELOG.md'))
assertThat(new File(basedir, 'module-b/CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'module-b/EXPECTED_CHANGELOG.md'))

// No root CHANGELOG.md when MODULE mode without BOM
assertThat(new File(basedir, 'CHANGELOG.md')).doesNotExist()

true
