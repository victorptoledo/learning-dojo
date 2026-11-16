---
from: python
status: draft
concept: "Exceptions, settings and tests"
from_way: "Exception hierarchies, raise ... from err, pydantic-settings validating the environment, and pytest with fixtures, monkeypatch and freezegun."
native_way: "Error classes with cause, a zod schema (or hand-written checks) over process.env, and node:test with hooks, t.mock.fn, t.mock.method and t.mock.timers."
sensei_tip: "raise AppError(...) from err is new AppError(..., { cause: err }). pytest fixtures become before/beforeEach hooks, and freezegun becomes t.mock.timers."
---

| Python | Node.js |
| --- | --- |
| `raise X("...") from err` | `throw new X("...", { cause: err })` |
| `pydantic-settings` `BaseSettings` | `Config.parse(process.env)` (zod) |
| `pytest.raises(ValueError)` | `assert.throws(fn, RangeError)` |
| `monkeypatch.setattr` | `t.mock.method(obj, "name")` |
| `freezegun` / `time-machine` | `t.mock.timers` |
| `sys.excepthook` | `process.on("uncaughtException")` |
