---
"reactivewizard-parent": patch
---

Implemented testIdAttributeName config in PlaywrightConfig. You can now configure the test-id attribute name
that Playwright will search for in a tag using testIdAttributeName under the playwright config in config.yml.
This makes it easy to switch to the attribute name "data-test-id". Example:

```yaml
playwright:
  browser: chrome
  testIdAttributeName: "data-test-id"
  ...
```