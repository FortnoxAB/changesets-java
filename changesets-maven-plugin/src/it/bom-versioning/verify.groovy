import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("starter-a=2.1.0")
assertThat(versions).contains("starter-b=3.0.1")
assertThat(versions).contains("bom=0.4.0")
// Consumer parent and root are not in VERSIONS
assertThat(versions).doesNotContain("consumer-parent=")
assertThat(versions).doesNotContain("bom-root=")

// Root version unchanged
def rootProject = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(rootProject.version).isEqualTo('1.0.0')

// BOM bumped to next-dev SNAPSHOT
def bom = new XmlSlurper().parse(new File(basedir, 'bom/pom.xml'))
assertThat(bom.version).isEqualTo('0.4.1-SNAPSHOT')
// BOM properties rewritten to the starters' next-dev SNAPSHOTs
assertThat(bom.properties.'starter-a.version'.text()).isEqualTo('2.1.1-SNAPSHOT')
assertThat(bom.properties.'starter-b.version'.text()).isEqualTo('3.0.2-SNAPSHOT')

// Consumer-parent has no own <version>, but its parent ref tracks the BOM
def consumerParent = new XmlSlurper().parse(new File(basedir, 'consumer-parent/pom.xml'))
assertThat(consumerParent.version.size()).isEqualTo(0)
assertThat(consumerParent.parent.artifactId.text()).isEqualTo('bom')
assertThat(consumerParent.parent.version.text()).isEqualTo('0.4.1-SNAPSHOT')

// Starters bumped to their own next-dev SNAPSHOTs
def starterA = new XmlSlurper().parse(new File(basedir, 'starter-a/pom.xml'))
assertThat(starterA.version).isEqualTo('2.1.1-SNAPSHOT')

def starterB = new XmlSlurper().parse(new File(basedir, 'starter-b/pom.xml'))
assertThat(starterB.version).isEqualTo('3.0.2-SNAPSHOT')

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

true
