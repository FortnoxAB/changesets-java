---
"reactivewizard-parent": minor
---

PlaywrightStoryReporter has been modified to work with JBehaves (see PLAT-3720).
In addition a configuration property saveTraces can be set to either none, failed (default) or all to determine when traces should be saved.
This should be set in the configuration that is used during testing, not the production one.