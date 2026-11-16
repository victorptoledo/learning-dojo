---
from: go
status: draft
concept: "Black-box tests of the UI"
from_way: "Go favours black-box tests in a _test package and httptest to check handlers through their public interface."
native_way: "Testing Library tests are black-box tests of a component: render it, use it through roles and labels, check what is shown."
sensei_tip: "Write UI tests the way you write httptest tests: exercise the public surface, assert on the response. For a component, the surface is the accessibility tree."
---

| Go | React |
| --- | --- |
| `package foo_test` black-box tests | Tests that query by role and text |
| `httptest.NewRecorder` | `render` into jsdom |
| `rr.Body.String()` contains | `screen.getByText(...)` |
| Fake interface at the edge | Fake fetch or loader prop |
| Retry loop with deadline | `waitFor` |
| Table-driven tests | One test per behaviour, data in arrays |
