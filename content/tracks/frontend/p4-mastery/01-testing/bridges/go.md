---
from: go
status: draft
concept: "Testing layers"
from_way: "go test with table-driven tests, httptest for handlers, chromedp or Playwright for browsers."
native_way: "The frontend stack: Vitest or node:test for logic, Playwright for the page in real browsers."
sensei_tip: "httptest.NewRecorder tests a handler without a network; page.route tests a page without a backend. Both make tests fast and deterministic."
---

| Go | In the browser |
| --- | --- |
| `go test` | Vitest / node:test |
| Table-driven tests | `test.each` / loops over cases |
| `httptest.NewServer` | `page.route` / a fake API in the test |
| chromedp | Playwright |
