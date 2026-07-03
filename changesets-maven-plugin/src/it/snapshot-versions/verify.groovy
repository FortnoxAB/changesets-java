import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

// SNAPSHOT pom = "next intended release". Patch confirms; minor/major escalate from non-boundary.
//   module-a 1.1.3-SNAPSHOT + patch  -> release 1.1.3 -> next dev 1.1.4-SNAPSHOT (no double-bump)
//   module-b 2.5.3-SNAPSHOT + minor  -> release 2.6.0 -> next dev 2.6.1-SNAPSHOT (escalates)

def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("module-a=1.1.3")
assertThat(versions).contains("module-b=2.6.0")

def moduleA = new XmlSlurper().parse(new File(basedir, 'module-a/pom.xml'))
assertThat(moduleA.version).isEqualTo('1.1.4-SNAPSHOT')

def moduleB = new XmlSlurper().parse(new File(basedir, 'module-b/pom.xml'))
assertThat(moduleB.version).isEqualTo('2.6.1-SNAPSHOT')

true
