import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

// Option #4 with a BOM. changesets:prepare rewrites all affected poms + BOM properties
// to unique next-dev SNAPSHOTs; release-plugin's policy resolution is unambiguous.

def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("starter-a=2.1.0")
assertThat(versions).contains("starter-b=3.0.5")
assertThat(versions).contains("bom=0.4.0")
assertThat(versions).doesNotContain("consumer-parent=")
assertThat(versions).doesNotContain("ptrp-bom-root=")

// Post-prepare poms — each at its unique next-dev SNAPSHOT.
def bom = new XmlSlurper().parse(new File(basedir, 'bom/pom.xml'))
assertThat(bom.version).isEqualTo('0.4.1-SNAPSHOT')

// BOM <properties> were rewritten by changesets:prepare (not by release-plugin this time).
assertThat(bom.properties.'starter-a.version'.text()).isEqualTo('2.1.1-SNAPSHOT')
assertThat(bom.properties.'starter-b.version'.text()).isEqualTo('3.0.6-SNAPSHOT')

def starterA = new XmlSlurper().parse(new File(basedir, 'starter-a/pom.xml'))
assertThat(starterA.version).isEqualTo('2.1.1-SNAPSHOT')

def starterB = new XmlSlurper().parse(new File(basedir, 'starter-b/pom.xml'))
assertThat(starterB.version).isEqualTo('3.0.6-SNAPSHOT')

def consumerParent = new XmlSlurper().parse(new File(basedir, 'consumer-parent/pom.xml'))
assertThat(consumerParent.version.size()).isEqualTo(0)
assertThat(consumerParent.parent.version.text()).isEqualTo('0.4.1-SNAPSHOT')

def rootProject = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(rootProject.version).isEqualTo('1.0.0-SNAPSHOT')

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

def buildLog = new File(basedir, "build.log").text
assert buildLog =~ /Resolved release version for bom from VERSIONS: 0\.4\.0/
assert buildLog =~ /Resolved release version for starter-a from VERSIONS: 2\.1\.0/
assert buildLog =~ /Resolved release version for starter-b from VERSIONS: 3\.0\.5/
assert !(buildLog =~ /Ambiguous VERSIONS lookup/) : "Expected no ambiguity in option-#4 flow"
assert !(buildLog =~ /useReleasePluginIntegration being set to true/) : \
    "This IT must NOT use useReleasePluginIntegration"

true
