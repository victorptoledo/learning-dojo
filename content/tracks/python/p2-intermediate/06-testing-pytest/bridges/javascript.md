---
from: javascript
status: draft
concept: "Jest or Vitest in pytest"
from_way: "describe and it blocks, expect(x).toBe(y) matchers, beforeEach and afterEach hooks, it.each tables, jest.fn and vi.mock module mocking, and snapshot tests."
native_way: "Flat test_ functions (or Test classes for grouping), plain assert, fixtures instead of hooks, parametrize instead of it.each, and Mock plus patch from unittest.mock."
sensei_tip: "vi.mock replaces a module for the whole file; patch replaces one name for one block or test. Patch narrowly, where the name is looked up, and prefer passing fakes in."
---

| Jest / Vitest | pytest |
| --- | --- |
| `it("adds vat", () => { ... })` | `def test_adds_vat():` |
| `expect(total).toBe(121)` | `assert total == 121` |
| `expect(() => f()).toThrow(/negative/)` | `with pytest.raises(ValueError, match="negative"):` |
| `beforeEach` / `afterEach` | a fixture with `yield` |
| `it.each([[1, 2], [3, 4]])` | `@pytest.mark.parametrize("a, b", [(1, 2), (3, 4)])` |
| `jest.fn().mockReturnValue(x)` | `Mock(return_value=x)` |
| `vi.mock("./mail")` | `patch("shop.checkout.send_email")` |
| `toBeCloseTo(0.3)` | `pytest.approx(0.3)` |
