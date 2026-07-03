# changesets

## 0.3.0

### Minor Changes

- Add support for handling dependency updates separately from other changesets, so that they can be presented in a more
  organised way.

  This is utilized by setting the update type to `dependency` in place of major/minor/patch.
- Add support for the `independent` version strategy, allowing sub-modules in a multi-module Maven project to be
  versioned independently of each other.

  The prepare→release handoff has moved from the single-line `.changeset/VERSION` file to a per-artifactId map in
  `.changeset/VERSIONS` (plural). The current version is now read directly from each module's `pom.xml` rather than from
  the version file. Any pre-existing `.changeset/VERSION` file is no longer consulted and can be safely deleted.

### Patch Changes

- - actions/checkout: 6.0.1
- - actions/setup-java: 5.1.0
- - actions/upload-artifact: 6.0.0
- - ch.qos.logback:logback-classic: 1.5.19
- - ch.qos.logback:logback-classic: 1.5.20
- - ch.qos.logback:logback-classic: 1.5.21
- - ch.qos.logback:logback-classic: 1.5.22
- - ch.qos.logback:logback-core: 1.5.19
- - ch.qos.logback:logback-core: 1.5.20
- - ch.qos.logback:logback-core: 1.5.21
- - com.diffplug.spotless:spotless-maven-plugin, org.apache.maven.plugins:maven-javadoc-plugin:
- - com.fasterxml.jackson.dataformat:jackson-dataformat-yaml: 2.19.2
- - com.fasterxml.jackson.dataformat:jackson-dataformat-yaml: 2.20.0
- - com.fasterxml.jackson.dataformat:jackson-dataformat-yaml: 2.20.1
- - commons-io:commons-io: 2.20.0
- - commons-io:commons-io: 2.21.0
- - dependabot/fetch-metadata: 2.4.0
- - github/codeql-action: 4.31.9
- - org.apache.maven.plugin-tools:maven-plugin-annotations: 3.15.2
  - org.apache.maven.plugins:maven-plugin-plugin: 3.15.2
  - org.apache.maven.plugins:maven-plugin-report-plugin: 3.15.2
- - org.apache.maven.plugins:maven-compiler-plugin, org.apache.maven.plugins:maven-surefire-plugin,
    com.diffplug.spotless:spotless-maven-plugin, org.apache.maven.plugins:maven-javadoc-plugin,
    org.sonatype.central:central-publishing-maven-plugin:
- - org.apache.maven.plugins:maven-jar-plugin, com.diffplug.spotless:spotless-maven-plugin:
- - org.apache.maven.release:maven-release-api: 3.2.0
- - org.apache.maven.release:maven-release-api: 3.3.1
- - org.apache.maven:maven-plugin-api: 3.9.11
  - org.apache.maven:maven-core: 3.9.11
  - org.apache.maven:maven-artifact: 3.9.11
  - org.apache.maven:maven-compat: 3.9.11
  - org.apache.maven.plugins:maven-invoker-plugin: 3.9.1
  - org.apache.maven.plugins:maven-gpg-plugin: 3.2.8
  - com.diffplug.spotless:spotless-maven-plugin: 2.46.0
- - org.apache.maven:maven-plugin-api: 3.9.12
  - org.apache.maven.plugins:maven-resources-plugin: 3.4.0
  - org.apache.maven.plugins:maven-source-plugin: 3.4.0
  - org.sonatype.central:central-publishing-maven-plugin: 0.10.0<
- - org.assertj:assertj-core: 3.27.4
- - org.assertj:assertj-core: 3.27.6
- - org.codehaus.mojo.versions:versions-common: 2.19.1
- - org.codehaus.mojo.versions:versions-common: 2.20.0
- - org.codehaus.mojo.versions:versions-common: 2.20.1
- - org.junit.jupiter:junit-jupiter-api, org.junit.jupiter:junit-jupiter-params: 5.13.4
- - org.junit.jupiter:junit-jupiter-api, org.junit.jupiter:junit-jupiter-params: 6.0.0
- - org.junit.jupiter:junit-jupiter-api, org.junit.jupiter:junit-jupiter-params: 6.0.1
- - org.junit.jupiter:junit-jupiter-api: 5.13.3
  - org.junit.jupiter:junit-jupiter-params: 5.13.3
- - org.mockito:mockito-core: 5.19.0
- - org.mockito:mockito-core: 5.20.0
- - ossf/scorecard-action: 2.4.3
- - step-security/harden-runner: 2.14.0
- - step-security/harden-runner: 2.14.0


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
