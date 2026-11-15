---
from: python
status: draft
concept: "pytest, mypy and ruff"
from_way: "pytest with fixtures and monkeypatch, mypy or pyright for types (assert_type, reveal_type), ruff for linting and formatting."
native_way: "node:test or Vitest, tsc for types (Expect<Equal> or expectTypeOf for type tests), typescript-eslint for linting, Prettier or Biome for formatting."
sensei_tip: "The stack maps almost one to one: pytest → node:test or Vitest, mypy → tsc, ruff check → ESLint (or Biome), ruff format → Prettier. typing.assert_type becomes Expect<Equal<...>>."
---

| Python | TypeScript |
| --- | --- |
| `def test_totals(): assert cart.total() == 300` | `test("totals", () => assert.equal(cart.total(), 300))` |
| `@pytest.fixture` | `beforeEach` or a helper function |
| `unittest.mock.Mock()` | `mock.fn()` / `vi.fn()` |
| `pytest.raises(ValueError)` | `assert.throws(() => ..., RangeError)` |
| `typing.assert_type(x, int)` | `Expect<Equal<typeof x, number>>` |
| `# type: ignore` | `// @ts-ignore` (avoid) / `// @ts-expect-error` |
| `ruff check` / `ruff format` | `eslint` / `prettier` or `biome` |
