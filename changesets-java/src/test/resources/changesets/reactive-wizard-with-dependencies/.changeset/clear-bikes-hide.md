---
"reactivewizard-parent": patch
---

Tests have been updated to use our standard, configured, ObjectMapper instead of using partially (or not at all) configured ObjectMapper instances. This
applies in particular use of constructors annotated with @Inject.
