---
from: go
status: draft
concept: "go test, go vet and staticcheck"
from_way: "go test with table-driven tests in _test.go files, go vet and staticcheck for patterns, gofmt for formatting; tests are compiled and type-checked."
native_way: "node:test or Vitest (table-driven loops work the same), typescript-eslint for patterns, Prettier or Biome for formatting, and tsc to type-check the tests."
sensei_tip: "Table-driven tests translate directly: a for...of over cases with test(name, ...) inside. gofmt's no-debate formatting is what Prettier gives you."
---

| Go | TypeScript |
| --- | --- |
| `func TestTotals(t *testing.T)` | `test("totals", () => ...)` |
| `for _, tc := range cases { t.Run(tc.name, ...) }` | `for (const c of cases) test(c.name, () => ...)` |
| `go vet`, `staticcheck` | `eslint` with typescript-eslint |
| `gofmt` | `prettier` / `biome format` |
| `go test -cover` | `node --test --experimental-test-coverage` |
