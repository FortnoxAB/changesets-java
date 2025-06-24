# Changesets

This is an attempt to bring the [changesets](https://github.com/changesets/changesets) way of working to the Java ecosystem,
to enable easier management of changelogs and semantically versioned releases.

Our goal is to keep compatibility with the files and formats of the original implementation as far as it makes sense, so that
users recognize the ways of working and feel at home.

This is it, at the moment. Stay tuned for more docs later on, thanks!



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

# Release Maven Plugin Integration

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