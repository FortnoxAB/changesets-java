# changesets

## 0.2.0

### Minor Changes

- Improved handling of multiline changesets, which previously would produce invalid bullet points.

  This change also introduces a Markdown formatter function to clean up any easily fixed formatting issues.
- Introduced more parameters for generating more flexible changesets using the add goal.

  You can now create changesets with more prefilled info and with predictable names (useful for testing and dependabot).

  The new parameters are `changesetLevel` and `changesetFile` (`changesetContent` was already available).

  ```
  mvn changesets:add -DchangesetLevel=patch -DchangesetContent="My change" -DchangesetFile=magic.md
  ```
- This change introduces a version policy (`ChangesetsVersionPolicy`) compatible with the Release Maven plugin, together
  with a flag `useReleasePluginIntegration` (default `false`) on the `prepare` goal. Enabling this flag will disable
  updating POM versions in `prepare` and only perform changeset processing. Updating versions in POMs can then be
  delegated to the Release plugin, together with all the other facilities that plugin provides.

### Patch Changes

- Adapt for breaking changes of versions-common (introduced in #47).


## 0.1.0

### Minor Changes

- Stop adding groupId of the module when adding new changesets using the `add` goal

### Patch Changes

- Dependency updates:
- Bump maven.version from 3.8.8 to 3.9.9
- Bump junit-jupiter.version from 5.11.1 to 5.11.2
- Bump org.slf4j:slf4j-api from 2.0.15 to 2.0.16
- Bump org.apache.maven.plugins:maven-invoker-plugin from 3.7.0 to 3.8.0
- Bump org.apache.maven.plugins:maven-install-plugin from 3.1.2 to 3.1.3

## 0.0.1

### Patch Changes

- Initial publishing to GitHub
