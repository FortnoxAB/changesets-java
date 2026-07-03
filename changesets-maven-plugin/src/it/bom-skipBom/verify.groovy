import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("starter-a=2.1.0")
assertThat(versions).contains("starter-b=3.0.1")
// BOM was NOT bumped under skipBom
assertThat(versions).doesNotContain("bom=")
assertThat(versions).doesNotContain("consumer-parent=")

// BOM pom completely untouched: version stays, properties stay
def bom = new XmlSlurper().parse(new File(basedir, 'bom/pom.xml'))
assertThat(bom.version).isEqualTo('0.3.0')
assertThat(bom.properties.'starter-a.version'.text()).isEqualTo('2.0.0')
assertThat(bom.properties.'starter-b.version'.text()).isEqualTo('3.0.0')

// Consumer-parent's parent ref also untouched (BOM didn't bump)
def consumerParent = new XmlSlurper().parse(new File(basedir, 'consumer-parent/pom.xml'))
assertThat(consumerParent.parent.version.text()).isEqualTo('0.3.0')

// Starters bumped as in plain independent mode
def starterA = new XmlSlurper().parse(new File(basedir, 'starter-a/pom.xml'))
assertThat(starterA.version).isEqualTo('2.1.1-SNAPSHOT')

def starterB = new XmlSlurper().parse(new File(basedir, 'starter-b/pom.xml'))
assertThat(starterB.version).isEqualTo('3.0.2-SNAPSHOT')

// Changelog is plain per-module sections (no consumer-parent wrapper, no pinned-versions block)
assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

true
