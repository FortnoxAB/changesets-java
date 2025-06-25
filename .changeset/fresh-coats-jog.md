---
"changesets": minor
---

Introduced more parameters for generating more flexible changesets using the add goal.

You can now create changesets with more prefilled info and with predictable names (useful for testing and dependabot).

The new parameters are `changesetLevel` and `changesetFile` (`changesetContent` was already available).
```
mvn changesets:add -DchangesetLevel=patch -DchangesetContent="My change" -DchangesetFile=magic.md
```

