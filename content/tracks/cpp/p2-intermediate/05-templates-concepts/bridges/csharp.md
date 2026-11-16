---
from: csharp
status: draft
concept: "Generics and constraints"
from_way: "Generics are reified at run time; constraints with where T : struct, IComparable<T>, new()."
native_way: "Templates are instantiated at compile time for every type; concepts express constraints, including operators and expressions."
sensei_tip: "C# generic math (INumber<T>) has its C++ counterpart in a Number concept built from std::integral and std::floating_point."
---

| C# | C++ |
| --- | --- |
| `where T : IComparable<T>` | `template <std::totally_ordered T>` |
| `where T : new()` | `std::default_initializable<T>` |
| `INumber<T>` (generic math) | a `Number` concept |
| JIT specialises value types | every type is specialised at compile time |
| `typeof(T)` at run time | `if constexpr` / type traits at compile time |
| `params T[]` | variadic templates |
