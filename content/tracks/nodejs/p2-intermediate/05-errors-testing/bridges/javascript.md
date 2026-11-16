---
from: javascript
status: draft
concept: "Testing without extra tools"
from_way: "Jest or Vitest with expect, jest.fn and fake timers, and errors shown in the browser console."
native_way: "node:test with node:assert, t.mock.fn and t.mock.timers built in, no configuration, and a process that must exit on bugs."
sensei_tip: "Jest's expect(x).toBe(y) is assert.equal(x, y); jest.fn() is t.mock.fn(); jest.useFakeTimers() is t.mock.timers.enable(). Vitest is still a good choice for front-end code; node:test removes a dependency on the server."
---

| Jest / Vitest | node:test |
| --- | --- |
| `expect(a).toBe(b)` | `assert.equal(a, b)` |
| `expect(a).toEqual(b)` | `assert.deepEqual(a, b)` |
| `await expect(p).rejects.toThrow()` | `await assert.rejects(p)` |
| `jest.fn()` | `t.mock.fn()` |
| `jest.useFakeTimers()` | `t.mock.timers.enable()` |
| `--coverage` | `--experimental-test-coverage` |
