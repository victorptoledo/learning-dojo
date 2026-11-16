---
from: javascript
status: draft
concept: "Functions and parameters"
from_way: "Functions are first-class values; objects are passed by sharing; undefined or null for no result; arrow functions capture variables by reference."
native_way: "Functions are free or members; parameters are copies unless declared references; std::optional for no result; lambdas pick copy or reference captures."
sensei_tip: "JS closures always capture by reference. In C++ choose: `[x]` freezes a copy, `[&x]` follows the variable and must not outlive it."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `(x) => x * 2` | `[](int x) { return x * 2; }` |
| objects passed by sharing | `T&` to share, `T` to copy |
| `undefined` | `std::optional<T>` / `std::nullopt` |
| destructuring `const {a, b} = f()` | `auto [a, b] = f();` |
| default params | default arguments |
| string or object enums | `enum class` |
| `Function` objects | `std::function<R(Args...)>` |
