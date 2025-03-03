---
"changesets": minor
---

This change introduces a version policy (`ChangesetsVersionPolicy`) compatible with the Release Maven plugin, together with a flag `useReleasePluginIntegration` (default `false`) on the `prepare` goal. Enabling this flag will disable updating POM versions in `prepare` and only perform changeset processing. Updating versions in POMs can then be delegated to the Release plugin, together with all the other facilities that plugin provides.
