import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

// Fixed group [module-a, module-b]: both bump (even though only module-a had a changeset).
// Base = max(1.0.0, 1.2.0) = 1.2.0; minor bump → 1.3.0 for both.
// module-c is not in the group and has no changeset → no bump.
def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("module-a=1.3.0")
assertThat(versions).contains("module-b=1.3.0")
assertThat(versions).doesNotContain("module-c=")
assertThat(versions).doesNotContain("fixed-root=")

def moduleA = new XmlSlurper().parse(new File(basedir, 'module-a/pom.xml'))
assertThat(moduleA.version).isEqualTo('1.3.1-SNAPSHOT')

def moduleB = new XmlSlurper().parse(new File(basedir, 'module-b/pom.xml'))
assertThat(moduleB.version).isEqualTo('1.3.1-SNAPSHOT')

def moduleC = new XmlSlurper().parse(new File(basedir, 'module-c/pom.xml'))
assertThat(moduleC.version).isEqualTo('5.0.0')

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

true
