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

## BOM (Bill of Materials) support

If your reactor contains a BOM — a `pom`-packaged module whose `<dependencyManagement>` pins sibling modules' versions via
`<properties>` (the Spring Boot convention) — opt in via `.changeset/config.json`:

```json
{
  "versioning": "independent",
  "bom": {
    "module": "fortnox-spring-boot-dependencies",
    "consumerParent": "fortnox-spring-boot-starter-parent"
  }
}
```

Behavior:

- **The BOM auto-bumps** at the max level of any tracked module's bump (any reactor module that pins through the BOM's
  `<dependencyManagement>`). Explicit changesets targeting the BOM still work — they combine with the synthesized level.
- **The BOM's `<properties>`** that pin sibling versions are rewritten on `prepare` (to the next `-SNAPSHOT`) and on
  `release` (to the release version). The mapping is discovered by walking the BOM's `<dependencyManagement>`; you don't
  have to name properties by any convention.
- **`consumerParent`** (optional) is the module a consumer sets as their `<parent>`. It typically has no `<version>` of its
  own and inherits from the BOM via Maven parent inheritance. When set, it is *excluded* from the plan (no own bump) and is
  used as the changelog header so consumers see entries named after the artifact they actually pin. Its `<parent>`
  reference is updated when the BOM bumps. Validation: the artifactId must exist in the reactor, and if it declares its own
  `<version>` it must match the BOM's.
- **Changelog rendering** in BOM mode collapses to a single top-level release header, with one sub-section per bumped
  module (including the BOM, which gets a synthesized `Pinned version updates` block listing the new sibling versions).

### Releasing a starter without cutting a BOM release

Pass `-DskipBom=true` to `changesets:prepare` to bypass BOM behavior for that invocation. The starters bump as in plain
independent mode, the BOM's pom is left untouched (version *and* pinned properties), and the changelog falls back to the
standard per-module sections. The `bom` block in `.changeset/config.json` stays in place — `skipBom` is a per-run override,
not a config change. Use it when you want to ship a quick starter patch between full BOM releases.

## Per-module changelogs

By default the release block is prepended to a single `CHANGELOG.md` at the reactor root. With `independent` versioning you
can instead emit one `CHANGELOG.md` per bumped submodule by setting `changelog: "module"`:

```json
{
  "versioning": "independent",
  "changelog": "module"
}
```

Behavior:

- Each module that bumps gets its own `CHANGELOG.md` next to its `pom.xml`, containing only that module's changes.
- Modules without changesets for the release are skipped — no empty file is created.
- No reactor-root `CHANGELOG.md` is written. Any existing root `CHANGELOG.md` is left untouched.
- If a [BOM](#bom-bill-of-materials-support) is also configured, the reactor-root `CHANGELOG.md` is *additionally* written as
  a rollup summary — one entry per bumped module under a single `consumer-parent@<bomVersion>` header, with each module's
  changes nested below — so consumers see a dependabot-style overview of what moved in that BOM release while the
  authoritative per-module history lives next to each module.

`changelog: "module"` requires `versioning: "independent"`; combining it with `fixed` is a config validation error, since all
modules would share one version anyway. Omitting `changelog` keeps the previous behavior (a single root `CHANGELOG.md`).

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

## Configuration reference

All settings below live in `.changeset/config.json` at the reactor root. The file itself is optional — omit it and the
defaults apply.

| Field                  | Type                 | Required | Default   | Allowed values                | Description                                                                                                                                  |
|------------------------|----------------------|----------|-----------|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `versioning`           | string               | No       | `fixed`   | `fixed`, `independent`        | Whether all reactor modules share one version (`fixed`) or each module tracks its own (`independent`).                                       |
| `linked`               | array of string arrays | No     | `[]`      | Reactor `artifactId`s          | Groups whose members bump together to the same new version *when at least one member has a changeset*. Requires `versioning: independent`.   |
| `fixed`                | array of string arrays | No     | `[]`      | Reactor `artifactId`s          | Groups whose members always bump together, even members without their own changesets. Requires `versioning: independent`.                    |
| `changelog`            | string               | No       | `root`    | `root`, `module`              | `root` writes one aggregated `CHANGELOG.md` at the reactor root. `module` writes one `CHANGELOG.md` per bumped submodule; requires `versioning: independent`. With a BOM also configured, a root rollup `CHANGELOG.md` is written in addition. |
| `bom.module`           | string               | Yes¹     | —         | Reactor `artifactId`           | The BOM module that pins sibling versions via `<properties>` in its `<dependencyManagement>`. Required when the `bom` block is present.      |
| `bom.consumerParent`   | string               | No       | *(unset)* | Reactor `artifactId`           | The pom-packaged module consumers set as their `<parent>`. Excluded from the bump plan; used as the changelog header. Must inherit its `<version>` from the BOM. |

¹ Required only when the `bom` block is present; the entire `bom` block is optional.

The same `artifactId` cannot appear in more than one `linked`/`fixed` group; that is a config validation error.

### CLI / plugin parameters

These flags are passed to `changesets:prepare` on the command line (or via `<configuration>`); they are *not* part of
`config.json`.

| Parameter                     | Type    | Default | Description                                                                                                                                              |
|-------------------------------|---------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `useReleasePluginIntegration` | boolean | `false` | When `true`, `prepare` writes `.changeset/VERSIONS` but does not modify any poms — version updates are delegated to maven-release-plugin via `ChangesetsVersionPolicy`. |
| `skipBom`                     | boolean | `false` | Per-invocation override that ignores the `bom` block in `config.json`. The BOM is not auto-bumped, its `<properties>` are not rewritten, and the changelog falls back to plain multi-module mode. |