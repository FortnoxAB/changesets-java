import groovy.xml.XmlSlurper

import static org.assertj.core.api.Assertions.assertThat

// -----------------------------------------------------------------------------
// Option #4: the recommended flow when combining changesets with release-plugin.
//
// Step 1 — `changesets:prepare` (no useReleasePluginIntegration):
//   Bumps each affected module's pom to its next-dev SNAPSHOT (unique per module).
//   Writes .changeset/VERSIONS with the release-target versions.
//   Writes CHANGELOG.md and deletes changesets.
//
// Step 2 — `release:update-versions`:
//   Consults ChangesetsVersionPolicy per module. Because each module now has a
//   unique -SNAPSHOT, `identifyModule` returns a single candidate every time —
//   no ambiguity even under independent versioning.
//   For dev-version: policy returns nextDevelopmentVersion(release) which equals
//   the current SNAPSHOT, so poms don't change again.
// -----------------------------------------------------------------------------

// VERSIONS holds the release-target versions from changesets:prepare.
def versions = new File(basedir, '.changeset/VERSIONS').text
assertThat(versions).contains("module-a=2.1.0")
assertThat(versions).contains("module-b=3.0.5")
assertThat(versions).doesNotContain("ptrp-root=")

// After changesets:prepare, each module is at its own next-dev SNAPSHOT:
//   module-a 2.0.5-SNAPSHOT + minor -> release 2.1.0 -> next-dev 2.1.1-SNAPSHOT
//   module-b 3.0.5-SNAPSHOT + patch -> release 3.0.5 -> next-dev 3.0.6-SNAPSHOT
def moduleA = new XmlSlurper().parse(new File(basedir, 'module-a/pom.xml'))
assertThat(moduleA.version).isEqualTo('2.1.1-SNAPSHOT')

def moduleB = new XmlSlurper().parse(new File(basedir, 'module-b/pom.xml'))
assertThat(moduleB.version).isEqualTo('3.0.6-SNAPSHOT')

// Root wasn't targeted by any changeset -> unchanged.
def rootProject = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assertThat(rootProject.version).isEqualTo('1.0.0-SNAPSHOT')

assertThat(new File(basedir, 'CHANGELOG.md'))
        .hasSameTextualContentAs(new File(basedir, 'EXPECTED_CHANGELOG.md'))

// The policy identified each module uniquely — no ambiguity warning, no prompt.
def buildLog = new File(basedir, "build.log").text
assert buildLog =~ /Resolved release version for module-a from VERSIONS: 2\.1\.0/
assert buildLog =~ /Resolved release version for module-b from VERSIONS: 3\.0\.5/
assert !(buildLog =~ /Ambiguous VERSIONS lookup/) : "Expected no ambiguity in option-#4 flow"
// Root is not in VERSIONS -> policy leaves it alone; that's expected.
assert buildLog =~ /No VERSIONS match for version 1\.0\.0-SNAPSHOT; leaving version unchanged/

// Sanity: no "Changesets processed, but not updating POMs" line — that would mean
// useReleasePluginIntegration snuck in, which is the *wrong* flow for this IT.
assert !(buildLog =~ /useReleasePluginIntegration being set to true/) : \
    "This IT must NOT use useReleasePluginIntegration; option #4 relies on changesets:prepare touching the poms"

true
