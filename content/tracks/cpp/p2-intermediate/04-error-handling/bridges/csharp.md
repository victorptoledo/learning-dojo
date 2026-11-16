---
from: csharp
status: draft
concept: "Exceptions and error results"
from_way: "Exceptions with try/catch/finally and when filters, the Try pattern (TryParse), and result types in some libraries."
native_way: "Exceptions with try/catch and RAII instead of finally, noexcept, and std::expected, which is the Try pattern with the error in the type."
sensei_tip: "`bool TryParse(string, out int)` becomes `std::expected<int, ParseError> parse(std::string_view)`."
---

| C# | C++ |
| --- | --- |
| `throw new ArgumentException()` | `throw std::invalid_argument(...);` |
| `catch (IOException e) when (...)` | `catch (const std::ios_base::failure& e)` |
| `finally` | destructors |
| `int.TryParse(s, out var n)` | `std::from_chars` / `std::expected<int, E>` |
| `throw;` | `throw;` |
| exception filters | no filters: catch and rethrow |
