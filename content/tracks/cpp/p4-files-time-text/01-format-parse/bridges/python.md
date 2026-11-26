---
from: python
status: draft
concept: "Formatting and parsing text"
from_way: "f-strings and str.format with the same mini-language, __format__ for your own types, int() and float() that raise ValueError."
native_way: "std::format borrowed Python's mini-language almost exactly; std::formatter is __format__, and from_chars replaces int() and float()."
sensei_tip: "Your Python format specs work unchanged: {:>8.2f}, {:#x}, {:*^9}. The difference is that C++ checks them at compile time."
---

| Python | C++ |
| --- | --- |
| `f"{total:>8.2f}"` | `std::format("{:>8.2f}", total)` |
| `print(f"...")` | `std::println("...", args...)` |
| `def __format__(self, spec)` | `std::formatter<T>::parse` + `format` |
| `int("12")` | `std::from_chars` + a check that ptr reached the end |
| `repr(0.1)` is `0.1` | `std::format("{}", 0.1)` is `0.1` |
