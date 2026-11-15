---
from: csharp
status: draft
concept: "xUnit, analyzers and the compiler"
from_way: "xUnit or NUnit with Moq, Roslyn analyzers and nullable warnings at build time; tests are compiled with the project."
native_way: "node:test or Vitest, tsc over source and tests, typescript-eslint as the analyzer; type-level tests check generic APIs."
sensei_tip: "Roslyn analyzers run inside the compiler; typescript-eslint is a separate tool that asks the compiler for types. Treat its warnings like analyzer warnings: errors in CI."
---

| C# | TypeScript |
| --- | --- |
| `[Fact] public void Totals()` | `test("totals", ...)` |
| `Assert.Equal(300, total)` | `assert.equal(total, 300)` |
| `new Mock<IListener>()` | `mock.fn()` |
| Roslyn analyzers (CA2007, async rules) | `no-floating-promises`, `no-misused-promises` |
| `dotnet test` | `node --test` / `vitest run` |
