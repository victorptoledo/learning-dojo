---
from: javascript
status: draft
concept: "Jest or Vitest tests"
from_way: "describe and it blocks, expect(...).toBe matchers, test.each for tables, jest.mock and spies, fake timers, and benchmark libraries."
native_way: "Test functions with plain comparisons, t.Run for nesting and tables, fakes through interfaces instead of module mocking, synctest for fake time, and built-in benchmarks and fuzzing."
sensei_tip: "Go cannot mock a module import. That limit pushes you to pass dependencies in, which is what makes Go code easy to test without magic."
---

| Jest / Vitest | Go |
| --- | --- |
| `test("discount", () => ...)` | `func TestDiscount(t *testing.T)` |
| `expect(d).toBe(900)` | `if d != 900 { t.Errorf(...) }` |
| `describe("price", ...)` | `t.Run("price", ...)` |
| `test.each(table)` | a table and a loop with `t.Run` |
| `jest.mock("./db")` | pass a fake that satisfies the interface |
| `jest.useFakeTimers()` | `testing/synctest` |
| `afterEach` | `t.Cleanup` |
| `--coverage` | `go test -cover` |
| snapshot tests | golden files in `testdata/` |
