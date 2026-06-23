# Changesets

This is an attempt to bring the [changesets](https://github.com/changesets/changesets) way of working to the Java ecosystem,
to enable easier management of changelogs and semantically versioned releases.

Our goal is to keep compatibility with the files and formats of the original implementation as far as it makes sense, so that
users recognize the ways of working and feel at home.

This is it, at the moment. Stay tuned for more docs later on, thanks!

## Versioning strategies

By default every module in the Maven reactor shares one version (the **fixed** strategy) — any changeset bumps every module
to the same new version. This matches the typical Maven convention where child modules inherit `<version>` from the parent.

To opt into per-module versioning, drop a `.changeset/config.json` at the reactor root:

```json
{
  "versioning": "independent",
  "linked": [["module-a", "module-b"]],
  "fixed":  [["module-c", "module-d"]]
}
```

- **`fixed` (default)** — entire reactor bumps as one.
- **`independent`** — each Maven module tracks its own version. Optional `linked` and `fixed` arrays group some modules:
  - **`linked` group**: members that have changesets bump together to the same new version; members without changesets stay where they are. The new version is the *highest current version in the group* bumped by the *highest level among changesets touching the group*.
  - **`fixed` group**: every member of the group bumps together, even members without their own changesets.

A changeset's frontmatter lists modules by `artifactId`:

```
---
"module-a": minor
"module-b": patch
---

Description of the change.
```

The same `artifactId` cannot appear in more than one group; that is a config validation error.

### Independent versioning and `<version>` declarations

For `independent` (or `linked` / `fixed` sub-groups) to actually update individual submodule versions, each Maven submodule
must declare its own `<version>` in its `pom.xml`. Submodules that inherit `<version>` from the parent only bump together
with the parent.

## How `prepare` and `release` interact

`changesets:prepare` (aggregator goal, runs once at the reactor root) reads all changesets in `.changeset/`, computes a new
version per affected module, writes the new release versions to `.changeset/VERSIONS` (a properties file keyed by
artifactId), updates each affected submodule's pom to the next `*-SNAPSHOT`, and prepends a release block to the root
`CHANGELOG.md`.

`changesets:release` reads `.changeset/VERSIONS` and writes each module's pom to its release version. The `.changeset/VERSIONS`
file is the handoff between the two goals.

## Dependency updates
Due to the way automated dependency update bots like Dependabot and Renovate work, there is often a large influx of automated changesets that are not easy to merge into the normal changelog. They can also be the source of an unwanted amount of noise in the changelog.

To help with this, we have added a feature to mark changesets as dependency update using the update type "dependency". This will make the changeset appear in a separate section in the changelog, and will be added as a single list of updates in the end of the released version.

Dependencies that have been updated to new versions multiple times between releases will have each of the updates listed in the changelog.

```
---
"changesets-java": dependency
---

- ch.qos.logback:logback-core: 1.5.12
- com.google.errorprone:error_prone_annotations: 2.34.0
```

## Release Maven Plugin Integration

To delegate versioning to the Release Maven Plugin, you can use the `ChangesetsVersionPolicy` together with the `useReleasePluginIntegration` flag:

```
<build>
  <plugins>
    <plugin>
      <groupId>se.fortnox.changesets</groupId>
      <artifactId>changesets-maven-plugin</artifactId>
      <version>${changesets.plugin.version}</version>
      <configuration>
        <useReleasePluginIntegration>true</useReleasePluginIntegration> <!-- Disables version updates in prepare goal -->
      </configuration>
    </plugin>
    <plugin>
      <artifactId>maven-release-plugin</artifactId>
      <version>3.1.1</version>
      <configuration>
        <projectVersionPolicyId>changesets</projectVersionPolicyId>
        <tagNameFormat>v@{project.version}</tagNameFormat>
      </configuration>
      <dependencies>
        <dependency>
          <groupId>se.fortnox.changesets</groupId>
          <artifactId>changesets-maven-plugin</artifactId>
          <version>${changesets.plugin.version}</version>
        </dependency>
      </dependencies>
    </plugin>
  </plugins>
```

Goals should then be invoked as `changesets:prepare release:prepare release:perform`. `changesets:release` should *not* be used.

With `useReleasePluginIntegration=true`, `changesets:prepare` writes `.changeset/VERSIONS` but does not modify any poms.
The maven-release-plugin then consults `ChangesetsVersionPolicy`, which reads `VERSIONS` and resolves the release / next
development version *per module* by `artifactId`. Modules not present in `VERSIONS` keep their current version unchanged.