---
from: csharp
status: draft
concept: "Formatting and parsing text"
from_way: "Interpolated strings and composite formatting ({0,-20} {1,8:F2}), IFormattable, and int.TryParse with CultureInfo.InvariantCulture."
native_way: "std::format with {:<20} {:>8.2f}, std::formatter for your types, and std::from_chars, which is always culture-invariant."
sensei_tip: "from_chars is int.TryParse with InvariantCulture built in, and without the allocation."
---

| C# | C++ |
| --- | --- |
| `$"{name,-20} {total,8:F2}"` | `std::format("{:<20} {:>8.2f}", name, total)` |
| `Console.WriteLine(...)` | `std::println(...)` |
| `IFormattable.ToString(format, provider)` | `std::formatter<T>` |
| `int.TryParse(s, NumberStyles.None, CultureInfo.InvariantCulture, out v)` | `std::from_chars(first, last, v)` |
| `string.Create` / `TryFormat` into a span | `std::format_to_n` |
