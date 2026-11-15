---
from: javascript
status: draft
concept: "Typed catch clauses and chaining"
from_way: "try/catch catching everything, instanceof checks inside catch, throw new Error(msg, { cause }), AggregateError from Promise.any, and any value can be thrown."
native_way: "Several except clauses that each catch a type, raise ... from error for the cause, ExceptionGroup and except* for several errors, and only exceptions can be raised."
sensei_tip: "Python's except matches by type, so the instanceof ladder inside a JavaScript catch becomes one except clause per type, and anything unmatched propagates on its own."
---

| JavaScript | Python |
| --- | --- |
| `try { } catch (e) { if (e instanceof TypeError) ... else throw e }` | `try: ... except TypeError: ...` |
| `throw new Error("x", { cause: e })` | `raise RuntimeError("x") from e` |
| `class NotFound extends Error {}` | `class NotFound(LookupError): ...` |
| `AggregateError` | `ExceptionGroup` |
| `finally` | `finally` (plus `else`) |
| `throw "oops"` (any value) | only `BaseException` subclasses can be raised |
| `JSON.parse` throws `SyntaxError` | `json.loads` raises `json.JSONDecodeError` (a `ValueError`) |
