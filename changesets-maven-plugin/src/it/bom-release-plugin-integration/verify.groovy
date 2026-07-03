import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

// Combined flow: `changesets:prepare -DuseReleasePluginIntegration=true` writes VERSIONS
// (no pom edits), then `release:update-versions` consults ChangesetsVersionPolicy to bump
// each module. For the BOM's <properties> that pin reactor artifacts through
// <dependencyManagement>, the release-plugin's own rewriter follows the ${prop}
// references and updates the property values — no changesets code needed there.
//
// Modules not present in VERSIONS (root, consumer-parent) are left unchanged by the policy.

def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("starter-a=2.1.0")
assertThat(versions).contains("starter-b=3.0.5")
assertThat(versions).contains("bom=0.4.0")
assertThat(versions).doesNotContain("consumer-parent=")
assertThat(versions).doesNotContain("bom-rp-root=")

// BOM bumped to next-dev SNAPSHOT.
def bom = new XmlSlurper().parse(new File(basedir, 'bom/pom.xml'))
assertThat(bom.version).isEqualTo('0.4.1-SNAPSHOT')

// BOM <properties> that pin reactor artifacts are updated in step by the release-plugin
// rewriter — the sibling starters' next-dev SNAPSHOTs are reflected here.
assertThat(bom.properties.'starter-a.version'.text()).isEqualTo('2.1.1-SNAPSHOT')
assertThat(bom.properties.'starter-b.version'.text()).isEqualTo('3.0.6-SNAPSHOT')

def starterA = new XmlSlurper().parse(new File(basedir, 'starter-a/pom.xml'))
assertThat(starterA.version).isEqualTo('2.1.1-SNAPSHOT')

def starterB = new XmlSlurper().parse(new File(basedir, 'starter-b/pom.xml'))
assertThat(starterB.version).isEqualTo('3.0.6-SNAPSHOT')

// Consumer-parent has no own <version> but its parent ref tracks the BOM.
def consumerParent = new XmlSlurper().parse(new File(basedir, 'consumer-parent/pom.xml'))
assertThat(consumerParent.version.size()).isEqualTo(0)
assertThat(consumerParent.parent.version.text()).isEqualTo('0.4.1-SNAPSHOT')

// Root not in VERSIONS -> unchanged.
def rootProject = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(rootProject.version).isEqualTo('1.0.0-SNAPSHOT')

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

def buildLog = new File(basedir, "build.log").text
assert buildLog =~ /Changesets processed, but not updating POMs due to useReleasePluginIntegration being set to true/

true
