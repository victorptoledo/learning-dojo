---
from: java
status: draft
concept: "Formatting and parsing text"
from_way: "String.format and printf with %-style specifiers checked at run time, Integer.parseInt and Double.parseDouble that throw NumberFormatException."
native_way: "std::format with {} fields checked at compile time, and std::from_chars that returns an error code and where it stopped."
sensei_tip: "Where Java throws on \"12abc\", from_chars succeeds on \"12\" and points at 'a': compare ptr with the end yourself."
---

| Java | C++ |
| --- | --- |
| `String.format("%-20s %8.2f", n, t)` | `std::format("{:<20} {:>8.2f}", n, t)` |
| `System.out.printf(...)` | `std::print(...)` / `std::println(...)` |
| `toString()` override | `template <> struct std::formatter<T>` |
| `Integer.parseInt(s)` | `std::from_chars(first, last, value)` |
| `NumberFormatException` | `std::errc::invalid_argument` / `result_out_of_range` |
