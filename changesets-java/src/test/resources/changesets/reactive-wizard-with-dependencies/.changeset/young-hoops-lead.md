---
"reactivewizard-parent": patch
---

We now have a common dependency for Jackson-related classes and configuration. This module provides a single point of configuration for an `ObjectMapper` (via
the `StandardObjectMapperFactory` class) that is suitable for HTTP request/response and event serialization/deserialization. This ensures that the
`ObjectMapper` is configured consistently across the application. Previously, this was set up in the `reactivewizard-core` module via FnxJaxRsModule.

