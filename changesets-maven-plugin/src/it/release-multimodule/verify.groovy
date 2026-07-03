import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

String expectedVersion = '2.5.0'

def project = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(project.version).isEqualTo(expectedVersion)

// Submodules inherit version via <parent>; check the parent ref was synced
def submodule1 = new XmlSlurper().parse(new File(basedir, 'module1/pom.xml'))
assertThat(submodule1.parent.version).isEqualTo(expectedVersion)

def submodule2 = new XmlSlurper().parse(new File(basedir, 'module2/pom.xml'))
assertThat(submodule2.parent.version).isEqualTo(expectedVersion)

true
