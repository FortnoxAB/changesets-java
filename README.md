# Changesets

This is an attempt to bring the [changesets](https://github.com/changesets/changesets) way of working to the Java ecosystem,
to enable easier management of changelogs and semantically versioned releases.

Our goal is to keep compatibility with the files and formats of the original implementation as far as it makes sense, so that
users recognize the ways of working and feel at home.

This is it, at the moment. Stay tuned for more docs later on, thanks!

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