---
from: java
status: draft
concept: "Test through the public API"
from_way: "Good JUnit tests use a class's public API and assert on results; Selenium or Playwright tests find elements by id or CSS and click them."
native_way: "Testing Library tests use the UI's public API: roles, labels and text, the same things a screen reader uses; user-event drives it like a person."
sensei_tip: "Treat accessible roles and names as the public API of a component. A test that uses a CSS class is like a JUnit test that reads a private field with reflection."
---

| Java | React |
| --- | --- |
| JUnit + AssertJ | node:test / Vitest + Testing Library |
| Test via public methods | Query by role, label, text |
| Reflection on private fields | Reading component state (avoid) |
| Mockito for collaborators at the edge | Mock fetch or pass a fake loader |
| Awaitility `await().until(...)` | `waitFor`, `findBy...` |
| Selenium by CSS selector | `getByRole("button", { name })` |
