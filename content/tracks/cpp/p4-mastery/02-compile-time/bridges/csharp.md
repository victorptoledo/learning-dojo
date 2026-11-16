---
from: csharp
status: draft
concept: "Compile-time programming"
from_way: "const for literals, source generators for code generation, typeof and reflection at run time."
native_way: "constexpr evaluates real code in the compiler, consteval forces it, static_assert checks it, type traits replace typeof checks."
sensei_tip: "Many uses of source generators and reflection become constexpr code and type traits in C++."
---

| C# | C++ |
| --- | --- |
| `const int Size = 64;` | `constexpr int kSize = 64;` |
| `static readonly` tables | `constexpr std::array` |
| source generators | templates, `constexpr`, C++26 reflection |
| `Debug.Assert` | `static_assert` (compile time) |
| `typeof(T) == typeof(int)` | `std::is_same_v<T, int>` |
