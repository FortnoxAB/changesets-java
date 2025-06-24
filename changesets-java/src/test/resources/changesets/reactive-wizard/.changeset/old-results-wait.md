---
"reactivewizard-parent": minor
---

Made EventListenerImpl aware of the `fnxtype` header in order to exit event handler dispatching early if the event type is not of interest to the listener. We
fall back to deserializing the body if the `fnxtype` header is not present and matching on the event `type` property. In addition we have introduced two new
Prometheus metrics (`event_type_selection_duration_seconds`, `event_type_selection_duration_seconds_count`) to track the adoption and efficiency of the
new feature. Labels for topic and type are included in addition to the boolean `header` label to indicate the source of the event type.

