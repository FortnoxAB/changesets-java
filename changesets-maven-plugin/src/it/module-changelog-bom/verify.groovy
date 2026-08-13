import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("starter-a=2.1.0")
assertThat(versions).contains("starter-b=3.0.1")
assertThat(versions).contains("bom=0.4.0")

// BOM bumped to next-dev SNAPSHOT, properties rewritten
def bom = new XmlSlurper().parse(new File(basedir, 'bom/pom.xml'))
assertThat(bom.version).isEqualTo('0.4.1-SNAPSHOT')
assertThat(bom.properties.'starter-a.version'.text()).isEqualTo('2.1.1-SNAPSHOT')
assertThat(bom.properties.'starter-b.version'.text()).isEqualTo('3.0.2-SNAPSHOT')

// Starters bumped to their own next-dev SNAPSHOTs
def starterA = new XmlSlurper().parse(new File(basedir, 'starter-a/pom.xml'))
assertThat(starterA.version).isEqualTo('2.1.1-SNAPSHOT')

def starterB = new XmlSlurper().parse(new File(basedir, 'starter-b/pom.xml'))
assertThat(starterB.version).isEqualTo('3.0.2-SNAPSHOT')

// Per-module changelogs written next to each starter's pom
assertThat(new File(basedir, 'starter-a/CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'starter-a/EXPECTED_CHANGELOG.md'))
assertThat(new File(basedir, 'starter-b/CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'starter-b/EXPECTED_CHANGELOG.md'))

// No bom/CHANGELOG.md when BOM has no explicit changesets of its own
assertThat(new File(basedir, 'bom/CHANGELOG.md')).doesNotExist()

// Root CHANGELOG.md = the BOM rollup summary
assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

true
