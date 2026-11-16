---
from: python
status: draft
concept: "Testing layers"
from_way: "pytest for units and integration, pytest-django's client, Playwright for Python or Selenium for the browser."
native_way: "The same split in JavaScript: Vitest for units, Testing Library and Playwright for the DOM and full journeys."
sensei_tip: "If you used pytest-playwright, the API is the same in JavaScript: page.get_by_role is page.getByRole."
---

| Python | In the browser |
| --- | --- |
| pytest | Vitest / node:test |
| `freezegun` | `page.clock` / `vi.useFakeTimers()` |
| `responses` / `respx` mocking | `page.route` / MSW |
| pytest-playwright | Playwright Test |
