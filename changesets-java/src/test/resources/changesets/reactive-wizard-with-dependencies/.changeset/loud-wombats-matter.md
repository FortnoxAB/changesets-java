---
"reactivewizard-parent": patch
---

Suppressing CVE-2024-31141 (vulnerability in the Kafka client lib). The vulnerability does not
affect any Reactive Wizard services. This vulnerability is patched in 3.8.0, but we should avoid
upgrading the client library to a version greater than what the Kafka server is running on (3.5.2),
thus the suppression.
