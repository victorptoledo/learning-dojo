---
from: go
status: draft
concept: "Formatting and parsing text"
from_way: "fmt.Sprintf with verbs checked by go vet, the Stringer and Formatter interfaces, and strconv.Atoi and ParseFloat."
native_way: "std::format with fields checked by the compiler itself, std::formatter specialisations, and std::from_chars and to_chars."
sensei_tip: "strconv is the closest cousin of <charconv>: locale-free, explicit errors. from_chars just reports the stop position instead of failing on junk."
---

| Go | C++ |
| --- | --- |
| `fmt.Sprintf("%-20s %8.2f", n, t)` | `std::format("{:<20} {:>8.2f}", n, t)` |
| `fmt.Println` | `std::println` |
| `String() string` method | `std::formatter<T>` |
| `strconv.Atoi(s)` | `std::from_chars` |
| `strconv.FormatFloat(f, 'g', -1, 64)` | `std::to_chars` / `std::format("{}", f)` |
