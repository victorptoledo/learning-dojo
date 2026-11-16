---
from: python
status: draft
concept: "Exceptions and error results"
from_way: "Exceptions are the normal way to report failure (EAFP); try/except/else/finally; with blocks for cleanup."
native_way: "Exceptions for exceptional failures, std::expected for expected ones, RAII for cleanup, and noexcept for functions that must not throw."
sensei_tip: "Python's 'easier to ask forgiveness' style is expensive in C++ when the failure is common: return std::expected instead of throwing in a loop."
---

| Python | C++ |
| --- | --- |
| `raise ValueError("bad")` | `throw std::invalid_argument("bad");` |
| `except ValueError as e:` | `catch (const std::invalid_argument& e)` |
| `finally:` | a destructor (RAII) |
| `raise` (re-raise) | `throw;` |
| returning `None` on failure | `std::expected<T, E>` with a typed error |
| exceptions are cheap enough | throwing is expensive: not for control flow |
