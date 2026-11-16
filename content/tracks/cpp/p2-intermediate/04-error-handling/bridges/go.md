---
from: go
status: draft
concept: "Errors and exceptions"
from_way: "Errors are values returned last; errors.Is and errors.As inspect them; panic for bugs; defer and recover."
native_way: "std::expected carries a value or an error like (T, error); exceptions unwind for rare failures; destructors replace defer."
sensei_tip: "`(T, error)` maps to `std::expected<T, E>` and `if err != nil` to `if (!result)`; panic maps to an exception or an assertion."
---

| Go | C++ |
| --- | --- |
| `func parse(s string) (int, error)` | `std::expected<int, ParseError> parse(std::string_view)` |
| `if err != nil { return err }` | `if (!r) return std::unexpected(r.error());` |
| `panic("bug")` | `throw` or `assert` / `std::terminate` |
| `recover()` | `catch (...)` |
| `defer cleanup()` | a destructor |
| `errors.Is(err, ErrNotFound)` | compare an `enum class` error or `std::error_code` |
