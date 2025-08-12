---
"reactivewizard-parent": patch
---

* Added PlaywrightTracingHooks that starts and stops tracing each scenario. Failed scenarios are saved in `LaunchOptions::tracesDir`.
* LaunchOptions::tracesDir config is now set to `./target/traces` by default. If explicitly set to null then PlaywrightTracingHooks will not trace.
* PlaywrightTracingHooks can be turned off by setting the system property `enablePlaywrightTracingHooks` to `false`.