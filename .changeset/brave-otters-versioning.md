---
"changesets": minor
---

Add support for the `independent` version strategy, allowing sub-modules in a multi-module Maven project to be versioned independently of each other.

The prepare→release handoff has moved from the single-line `.changeset/VERSION` file to a per-artifactId map in `.changeset/VERSIONS` (plural). The current version is now read directly from each module's `pom.xml` rather than from the version file. Any pre-existing `.changeset/VERSION` file is no longer consulted and can be safely deleted.
