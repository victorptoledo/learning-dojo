---
from: javascript
status: draft
concept: "Exceptions and error results"
from_way: "throw any value, try/catch/finally, rejected promises, and result objects by convention."
native_way: "throw objects derived from std::exception, catch by const reference, RAII for cleanup, std::expected for typed results."
sensei_tip: "A `{ ok: false, error }` result object is exactly std::expected; the compiler now checks that callers look at it."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `throw new Error("bad")` | `throw std::runtime_error("bad");` |
| `catch (e)` (anything) | `catch (const std::exception& e)` |
| `finally { }` | destructors |
| `{ ok, value, error }` objects | `std::expected<T, E>` |
| `Promise.reject(err)` | `std::future` carrying an exception |
| TypeScript `never` for throws | `[[noreturn]]`; `noexcept` for no throws |
