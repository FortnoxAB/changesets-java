---
"reactivewizard-parent": patch
---

The metric "event_deserialization_duration_seconds" has been introduced to gather more insights about our event consumption, deserialization and execution
against event handlers. The
granularity is at topic and type level and is currently measured before topic and type filtering (which is a target for future improvements).
