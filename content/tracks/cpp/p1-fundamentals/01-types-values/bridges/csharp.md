---
from: csharp
status: draft
concept: "Values and references"
from_way: "struct types are values, class types are references; ref and out pass by reference; string is immutable."
native_way: "Every type behaves like a C# struct by default: copied on assignment; T& is like ref; std::string is a mutable value."
sensei_tip: "Think of every C++ class as a C# struct with deep copies, and use const references the way you would use `in` parameters."
---

| C# | C++ |
| --- | --- |
| `class` instance (reference) | any object (value); `std::unique_ptr` for heap ownership |
| `ref int x` | `int& x` |
| `in` / readonly ref | `const T&` |
| `checked` overflow | unsigned wraps, signed overflow is UB |
| `string`, `ReadOnlySpan<char>` | `std::string`, `std::string_view` |
| `const`, `readonly` | `constexpr`, `const` |
