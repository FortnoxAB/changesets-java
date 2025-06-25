---
"reactivewizard-parent": minor
---

Kafka messages transporting events have now been extended with an additional header of key `fnxtype`. The value is a copy of the `type` property from
the event. In the case where a `type` property is not found the header will have a null value. The prefix `fnx` is used to avoid potential future conflicts with
other headers.
