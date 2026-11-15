---
from: java
status: draft
concept: "JUnit, compile-time checks and static analysis"
from_way: "JUnit 5 with Mockito, javac as the type checker, and static analysis with SpotBugs, Error Prone or SonarQube; tests are compiled, so they are type-checked."
native_way: "node:test or Vitest with built-in mocks, tsc as the type checker (tests included, since runners only strip types), and typescript-eslint as the static analyser."
sensei_tip: "In Java the test sources compile or the build fails. In TypeScript, add test files to the tsc run explicitly, or a broken test may run and pass."
---

| Java | TypeScript |
| --- | --- |
| `@Test void totals()` | `test("totals", () => { ... })` |
| `assertEquals(300, cart.total())` | `assert.equal(cart.totalCents(), 300)` |
| `Mockito.mock(Listener.class)` | `mock.fn()` / `vi.fn()` |
| `@BeforeEach` | `beforeEach(() => ...)` |
| Error Prone, SpotBugs | typescript-eslint type-checked rules |
| `mvn verify` | `npm run check` (tsc + eslint + tests) |
