---
from: javascript
status: draft
concept: "Compile-time programming"
from_way: "TypeScript types are computed at compile time but erased; values are computed at run time."
native_way: "C++ computes both types (traits, concepts) and values (constexpr) at compile time, and the values remain in the program."
sensei_tip: "TypeScript conditional types are close to std::conditional_t and type traits; constexpr adds compile-time values, which TypeScript lacks."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| conditional types `T extends U ? X : Y` | `std::conditional_t<cond, X, Y>` |
| `as const` literals | `constexpr` values |
| build-time codegen scripts | `constexpr` functions |
| type guards | `if constexpr` + traits |
| `satisfies` | `static_assert` + concepts |
