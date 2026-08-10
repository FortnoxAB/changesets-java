import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

// Setup: module-a and module-b both at 1.0.5-SNAPSHOT (same current version).
// module-a minor -> 1.1.0, module-b major -> 2.0.0 (differing targets in VERSIONS).
//
// With useReleasePluginIntegration=true, changesets:prepare leaves the poms at
// 1.0.5-SNAPSHOT. When release-plugin then asks the policy for a version, the
// policy sees two candidates at 1.0.5-SNAPSHOT with different targets — genuinely
// ambiguous. The policy leaves the version unchanged and emits an actionable WARN.

// changesets:prepare wrote both targets to VERSIONS.
def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("module-a=1.1.0")
assertThat(versions).contains("module-b=2.0.0")

// Poms remain at their pre-prepare SNAPSHOT (prepare skipped pom edits under the flag,
// and release:update-versions kept them because the policy fell back on ambiguity).
def moduleA = new XmlSlurper().parse(new File(basedir, 'module-a/pom.xml'))
assertThat(moduleA.version).isEqualTo('1.0.5-SNAPSHOT')

def moduleB = new XmlSlurper().parse(new File(basedir, 'module-b/pom.xml'))
assertThat(moduleB.version).isEqualTo('1.0.5-SNAPSHOT')

// The build log contains the actionable ambiguity WARN — no silent guess.
def buildLog = new File(basedir, "build.log").text
assert buildLog =~ /Ambiguous VERSIONS lookup for version 1\.0\.5-SNAPSHOT/
assert buildLog =~ /differing targets/
assert buildLog =~ /independent versioning is not supported/
assert buildLog =~ /changesets:release/

// And it did NOT prompt (no InteractiveInputPrompt / "What is the release version" line).
assert !(buildLog =~ /What is the release version/) : \
    "The policy must not prompt; it should fall back to leaving the version unchanged."

true
