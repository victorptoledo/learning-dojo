---
from: python
status: draft
concept: "pytest for components"
from_way: "pytest tests call functions and assert results; Django's test client posts forms and checks the response; Playwright drives real browsers."
native_way: "Testing Library renders a component into jsdom, finds elements by role and label, and user-event types and clicks; assertions are about what is visible."
sensei_tip: "Write component tests like Django client tests: act through the interface (submit the form), assert on the output (the page shows the error), never on internals."
---

| Python | React |
| --- | --- |
| `pytest` fixtures | `render` with a providers wrapper |
| `client.post("/signup", data)` | `user.type(...)` then `user.click(...)` |
| `assert "Welcome" in response.text` | `screen.getByText("Welcome")` |
| `unittest.mock.patch` at the boundary | Fake `fetch` or a loader prop |
| Polling with timeout | `waitFor` |
| `axe-selenium-python` | `jest-axe`, axe DevTools |
