---
from: javascript
status: draft
concept: "Formatting and parsing text"
from_way: "Template literals, toFixed and padStart for layout, Number() and parseInt() that return NaN or stop at junk."
native_way: "std::format with width, fill and precision in one spec, and std::from_chars that reports exactly where it stopped."
sensei_tip: "parseInt(\"12abc\") is 12 and from_chars also stops after 12, but from_chars tells you so: check ptr."
---

| JavaScript | C++ |
| --- | --- |
| `` `${n.toFixed(2).padStart(8)}` `` | `std::format("{:>8.2f}", n)` |
| `console.log(...)` | `std::println(...)` |
| `toString()` / `Symbol.toPrimitive` | `std::formatter<T>` |
| `parseInt(s, 10)` | `std::from_chars(first, last, v, 10)` |
| `Number.isNaN(x)` | `ec != std::errc{}` |
