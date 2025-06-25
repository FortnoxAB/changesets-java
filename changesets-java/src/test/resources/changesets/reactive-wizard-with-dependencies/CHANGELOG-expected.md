# reactivewizard-parent

## 26.0.0

### Major Changes

- In addition to moving our open source edition of [Reactive Wizard](https://github.com/FortnoxAB/reactive-wizard) we
  have also adopted semantic versioning to our releases. This means that we will now be able to communicate the impact
  of changes in our releases more clearly.

### Minor Changes

- Added Metrics.name() method to more easily label named metrics.
- Added createPage(BrowserContext) that creates a page from the given context. The default page is also created from the
  BrowserContext API instead of the Page API.
- Added metrics to event publisher to investigate how often the auth is lost during a distributed chain of calls that
  results in events.
- Jackson dependencies have been removed from our reactivewizard-dates module. Dependent classes have been moved to the
  new reactivewizard-jackson module:
  - com.fortnox.reactivewizard.dates.RWDateFormat

  If reactivewizard-dates was previously used as a dependency to access RWDateFormat then you will need to change that
  dependency to reactivewizard-jackson, in all other cases it should be included transitively via reactivewizard-core.
- JsonDeserializerFactory has been extended with createJsonNodeMapper which can map a Jackson JsonNode to a bean
- Kafka messages transporting events have now been extended with an additional header of key `fnxtype`. The value is a
  copy of the `type` property from the event. In the case where a `type` property is not found the header will have a
  null value. The prefix `fnx` is used to avoid potential future conflicts with other headers.
- Made EventListenerImpl aware of the `fnxtype` header in order to exit event handler dispatching early if the event
  type is not of interest to the listener. We fall back to deserializing the body if the `fnxtype` header is not present
  and matching on the event `type` property. In addition we have introduced two new Prometheus metrics
  (`event_type_selection_duration_seconds`, `event_type_selection_duration_seconds_count`) to track the adoption and
  efficiency of the new feature. Labels for topic and type are included in addition to the boolean `header` label to
  indicate the source of the event type.
- PlaywrightStoryReporter has been modified to work with JBehaves (see PLAT-3720). In addition a configuration property
  saveTraces can be set to either none, failed (default) or all to determine when traces should be saved. This should be
  set in the configuration that is used during testing, not the production one.

### Patch Changes

- * Added PlaywrightTracingHooks that starts and stops tracing each scenario. Failed scenarios are saved in
    `LaunchOptions::tracesDir`.
  * LaunchOptions::tracesDir config is now set to `./target/traces` by default. If explicitly set to null then
    PlaywrightTracingHooks will not trace.
  * PlaywrightTracingHooks can be turned off by setting the system property `enablePlaywrightTracingHooks` to `false`.
- * Info-logging when trying to create a consumer and the Kafka client is disabled, to let the developer know that a
    consumer wont be created.
  * Confusing 'Listener with suffix ... subscribed to topics ...' Info-log is no longer logging when Kafka client is
    disabled because there is no actual subscription when the Kafka client is disabled.
- Added event IDs to events published by EventPublisherImpl.
- Added more sql statement debug logs.
- Added support for JAX-RS PATCH, alongside existing RW PATCH
- Adding CVE-2024-56128 to the CVE supression list.
- Configure blockhound to ignore getLogger as a blocking call.
- Downgraded micrometer-observation as reactor-kafka doesn't align versions with reactor-core-micrometer. Until
  https://github.com/reactor/reactor-kafka/issues/395 we will also have micrometer-observation disabled in our renovate
  configuration.
- Dummy changeset to release a new patch version.
- Fixed a typographical error in the RuntimeException message thrown by `ParameterizedQuery::getDynamicQueryPart`. The
  corrected message now reads: `Query contains placeholder "??" but the method does not have such an argument.`
- Implemented testIdAttributeName config in PlaywrightConfig. You can now configure the test-id attribute name that
  Playwright will search for in a tag using testIdAttributeName under the playwright config in config.yml. This makes it
  easy to switch to the attribute name "data-test-id". Example:

  ```yaml
  playwright:
    browser: chrome
    testIdAttributeName: "data-test-id"
    ...
  ```
- Improved RWStory to support Timescale-only service testing
- Refactored EventListenerImpl in preparation of having listeners accept messages as a Flux/Mono. An interim
  MessageToEvent class was created to abstract the conversion of messages to events. The change is internal to Reactive
  Wizard and should not affect any existing code.
- Suppressing CVE-2024-31141 (vulnerability in the Kafka client lib). The vulnerability does not affect any Reactive
  Wizard services. This vulnerability is patched in 3.8.0, but we should avoid upgrading the client library to a version
  greater than what the Kafka server is running on (3.5.2), thus the suppression.
- Tests have been updated to use our standard, configured, ObjectMapper instead of using partially (or not at all)
  configured ObjectMapper instances. This applies in particular use of constructors annotated with @Inject.
- The metric "event_deserialization_duration_seconds" has been introduced to gather more insights about our event
  consumption, deserialization and execution against event handlers. The granularity is at topic and type level and is
  currently measured before topic and type filtering (which is a target for future improvements).
- TransactionEventPublisherImpl now adds an eventId header in every published event.
- Upgraded Netty to 4.1.115 to patch vulnerability with unsafe reading of environment files(see CVE-2024-47535 for more
  info.). This vulnerability does not affect Reactive Wizard services.
- Use `testcontainers-bom`, instead of individual package versions.
- We now have a common dependency for Jackson-related classes and configuration. This module provides a single point of
  configuration for an `ObjectMapper` (via the `StandardObjectMapperFactory` class) that is suitable for HTTP
  request/response and event serialization/deserialization. This ensures that the `ObjectMapper` is configured
  consistently across the application. Previously, this was set up in the `reactivewizard-core` module via
  FnxJaxRsModule.

### Dependency Updates

- ch.qos.logback:logback-classic: 1.5.11
- ch.qos.logback:logback-classic: 1.5.12
- ch.qos.logback:logback-core: 1.5.11
- ch.qos.logback:logback-core: 1.5.12
- com.fasterxml.jackson:jackson-bom: 2.18.0
- com.fasterxml.jackson:jackson-bom: 2.18.2
- com.fortnox.maven:parent-pom: 21.14
- com.github.jnr:jnr-posix: 3.1.20
- com.github.luben:zstd-jni: 1.5.6-6
- com.github.luben:zstd-jni: 1.5.6-8
- com.google.code.gson:gson: 2.11.0
- com.google.errorprone:error_prone_annotations: 2.33.0
- com.google.errorprone:error_prone_annotations: 2.34.0
- com.google.errorprone:error_prone_annotations: 2.36.0
- com.google.guava:guava: 33.3.1-jre
- com.google.guava:guava: 33.4.0-jre
- com.mattbertolini:liquibase-slf4j: 5.1.0
- com.microsoft.playwright:playwright: 1.48.0
- com.microsoft.playwright:playwright: 1.49.0
- com.zaxxer:HikariCP: 6.2.1
- commons-codec:commons-codec: 1.17.1
- commons-io:commons-io: 2.17.0
- commons-io:commons-io: 2.18.0
- io.cucumber:cucumber-guice: 7.20.1
- io.cucumber:cucumber-java8: 7.20.1
- io.cucumber:cucumber-java: 7.20.1
- io.cucumber:cucumber-junit-platform-engine: 7.20.1
- io.cucumber:cucumber-junit: 7.20.1
- io.dropwizard.metrics:metrics-caffeine: 4.2.28
- io.dropwizard.metrics:metrics-caffeine: 4.2.29
- io.dropwizard.metrics:metrics-core: 4.2.28
- io.dropwizard.metrics:metrics-core: 4.2.29
- io.github.classgraph:classgraph: 4.8.177
- io.github.classgraph:classgraph: 4.8.179
- io.lettuce:lettuce-core: 6.4.0.RELEASE
- io.lettuce:lettuce-core: 6.5.1.RELEASE
- io.micrometer:micrometer-observation: 1.13.6
- io.micrometer:micrometer-observation: 1.14.2
- io.netty:netty-bom: 4.1.114.Final
- io.opentelemetry:opentelemetry-bom: 1.43.0
- io.opentelemetry:opentelemetry-bom: 1.44.1
- io.opentelemetry:opentelemetry-bom: 1.45.0
- io.projectreactor.tools:blockhound-junit-platform: 1.0.10.RELEASE
- io.projectreactor.tools:blockhound: 1.0.10.RELEASE
- io.projectreactor:reactor-bom: 2023.0.11
- io.projectreactor:reactor-bom: 2024.0.0
- io.projectreactor:reactor-bom: 2024.0.1
- io.swagger.core.v3:swagger-annotations: 2.2.25
- io.swagger.core.v3:swagger-annotations: 2.2.26
- io.swagger.core.v3:swagger-annotations: 2.2.27
- io.zipkin.reporter2:zipkin-reporter: 3.4.2
- io.zipkin.reporter2:zipkin-reporter: 3.4.3
- io.zipkin.zipkin2:zipkin: 3.4.2
- io.zipkin.zipkin2:zipkin: 3.4.3
- jakarta.inject:jakarta.inject-api: 2.0.1.MR
- maven: 3.9.9
- net.bytebuddy:byte-buddy: 1.15.10
- net.bytebuddy:byte-buddy: 1.15.11
- net.bytebuddy:byte-buddy: 1.15.5
- org.apache.commons:commons-csv: 1.12.0
- org.apache.commons:commons-lang3: 3.17.0
- org.apache.commons:commons-text: 1.12.0
- org.apache.commons:commons-text: 1.13.0
- org.apache.logging.log4j:log4j-api: 2.24.1
- org.apache.logging.log4j:log4j-api: 2.24.2
- org.apache.logging.log4j:log4j-api: 2.24.3
- org.apache.logging.log4j:log4j-core: 2.24.1
- org.apache.logging.log4j:log4j-core: 2.24.2
- org.apache.logging.log4j:log4j-core: 2.24.3
- org.apache.logging.log4j:log4j-jul: 2.24.1
- org.apache.logging.log4j:log4j-jul: 2.24.2
- org.apache.logging.log4j:log4j-jul: 2.24.3
- org.apache.logging.log4j:log4j-layout-template-json: 2.24.1
- org.apache.logging.log4j:log4j-layout-template-json: 2.24.2
- org.apache.logging.log4j:log4j-layout-template-json: 2.24.3
- org.apache.logging.log4j:log4j-slf4j2-impl: 2.24.1
- org.apache.logging.log4j:log4j-slf4j2-impl: 2.24.2
- org.apache.logging.log4j:log4j-slf4j2-impl: 2.24.3
- org.apache.maven.shared:maven-invoker: 3.3.0
- org.awaitility:awaitility: 4.2.2
- org.bouncycastle:bcpkix-jdk18on: 1.79
- org.bouncycastle:bcprov-jdk18on: 1.79
- org.checkerframework:checker-qual: 3.48.1
- org.checkerframework:checker-qual: 3.48.3
- org.codehaus.plexus:plexus-utils: 4.0.2
- org.hibernate.validator:hibernate-validator: 8.0.2.Final
- org.jetbrains.kotlin:kotlin-bom: 2.0.21
- org.jetbrains.kotlin:kotlin-bom: 2.1.0
- org.junit-pioneer:junit-pioneer: 2.3.0
- org.junit:junit-bom: 5.11.2
- org.junit:junit-bom: 5.11.3
- org.junit:junit-bom: 5.11.4
- org.mockito:mockito-core: 5.14.2
- org.mockito:mockito-junit-jupiter: 5.14.2
- org.postgresql:postgresql: 42.7.4
- org.scala-lang:scala-library: 2.13.15
- org.testcontainers:junit-jupiter: 1.20.2
- org.testcontainers:junit-jupiter: 1.20.4
- org.testcontainers:postgresql: 1.20.2
- org.testcontainers:postgresql: 1.20.4
- org.testcontainers:testcontainers: 1.20.2
- org.testcontainers:testcontainers: 1.20.4
- org.yaml:snakeyaml: 2.3

