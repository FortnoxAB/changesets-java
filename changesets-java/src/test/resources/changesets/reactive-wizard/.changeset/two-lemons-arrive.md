---
"reactivewizard-parent": minor
---

Jackson dependencies have been removed from our reactivewizard-dates module. Dependent classes have been moved to the new reactivewizard-jackson module:

- com.fortnox.reactivewizard.dates.RWDateFormat

If reactivewizard-dates was previously used as a dependency to access RWDateFormat then you will need to change that dependency to reactivewizard-jackson, in
all other cases it should be included transitively via reactivewizard-core.
