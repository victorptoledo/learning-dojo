---
from: java
status: draft
concept: "Testing layers"
from_way: "JUnit for units, Spring's MockMvc for slices, Testcontainers and Selenium for integration and end-to-end."
native_way: "Vitest or node:test for units, Testing Library or Playwright for components and pages, Playwright for end-to-end in three browsers."
sensei_tip: "MockMvc tests the web layer like a client would; Playwright with getByRole tests the UI like a user would. Same philosophy, one layer up."
---

| Java | In the browser |
| --- | --- |
| JUnit 6 | Vitest / node:test |
| MockMvc | Testing Library or Playwright component tests |
| WireMock | `page.route` or Mock Service Worker |
| Selenium WebDriver | Playwright |
