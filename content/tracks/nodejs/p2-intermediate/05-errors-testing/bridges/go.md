---
from: go
status: draft
concept: "Errors, config and tests"
from_way: "Errors as values wrapped with %w, panics for bugs, config read from the environment in main, and go test with table tests and httptest."
native_way: "Exceptions with cause for operational failures, crashes for bugs, config validated at startup, and node:test with assertions, mocks and a server on port 0."
sensei_tip: "Go's split between error and panic is the operational vs programmer split. Table-driven tests translate directly: a for loop over cases with a test() per case."
---

| Go | Node.js |
| --- | --- |
| `fmt.Errorf("x: %w", err)` | `new Error("x", { cause: err })` |
| `errors.Is` / `errors.As` | `error.code` / `instanceof` |
| `panic` for bugs | Let the process crash |
| `func TestX(t *testing.T)` | `test("x", (t) => {})` |
| `httptest.NewServer(h)` | `server.listen(0)` |
| `t.Run(name, ...)` table tests | `for (const c of cases) test(c.name, ...)` |
