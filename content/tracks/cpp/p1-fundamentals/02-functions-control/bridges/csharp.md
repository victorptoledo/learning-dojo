---
from: csharp
status: draft
concept: "Functions and parameters"
from_way: "Methods in classes; ref/out/in modifiers; Nullable<T> and tuples; lambdas and delegates capture variables by reference."
native_way: "Free functions; & and const& instead of ref and in; std::optional instead of T?; structured bindings for tuples; lambdas with explicit captures."
sensei_tip: "`out` parameters are rarely needed in C++: return a struct or a `std::optional` and unpack it."
---

| C# | C++ |
| --- | --- |
| `ref int x` / `in Point p` | `int& x` / `const Point& p` |
| `out int result` | return value (struct, `std::optional`) |
| `int?` | `std::optional<int>` |
| `(int, int) Bounds()` + deconstruction | `std::pair` or a struct + `auto [a, b]` |
| `Func<int, int>` | `std::function<int(int)>` or a template |
| `enum Level` | `enum class Level` |
