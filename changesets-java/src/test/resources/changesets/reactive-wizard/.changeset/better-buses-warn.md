---
"reactivewizard-parent": patch
---

* Info-logging when trying to create a consumer and the Kafka client is disabled, 
to let the developer know that a consumer wont be created.

* Confusing 'Listener with suffix ... subscribed to topics ...' Info-log is no longer logging when Kafka client 
is disabled because there is no actual subscription when the Kafka client is disabled. 