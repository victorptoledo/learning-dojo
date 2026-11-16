---
from: javascript
status: draft
concept: "Testing the DOM"
from_way: "Unit tests of functions and Node APIs with Jest, Vitest or node:test."
native_way: "DOM tests add user-like interaction, role-based queries, async waiting and accessibility checks, in jsdom or a real browser."
sensei_tip: "Most flaky UI tests come from waiting for time instead of for a condition; Playwright's auto-waiting locators fix most of them."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `expect(fn()).toBe(x)` | `await expect(locator).toHaveText(x)` |
| `jest.useFakeTimers()` | `page.clock.install()` |
| `nock` HTTP mocks | `page.route` / MSW |
| Snapshot of an object | Visual comparison with `toHaveScreenshot` |
