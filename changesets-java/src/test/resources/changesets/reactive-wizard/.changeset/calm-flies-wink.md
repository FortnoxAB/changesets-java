---
"reactivewizard-parent": patch
---

Refactored EventListenerImpl in preparation of having listeners accept messages as a Flux/Mono. An interim MessageToEvent class was created to abstract the
conversion of messages to events. The change is internal to Reactive Wizard and should not affect any existing code.
