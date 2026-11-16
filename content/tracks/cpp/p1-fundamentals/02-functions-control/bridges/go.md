---
from: go
status: draft
concept: "Functions and parameters"
from_way: "Functions return multiple values, often value, error; parameters are copies; closures capture variables by reference."
native_way: "Functions return one value (a struct, pair or optional); parameters are copies unless declared references; lambdas choose capture mode."
sensei_tip: "Replace `v, ok := f()` with `if (auto v = f()) { use(*v); }` using std::optional."
---

| Go | C++ |
| --- | --- |
| `func area(w, h int) int` | `int area(int w, int h)` |
| `v, ok := m[k]` | `if (auto it = m.find(k); it != m.end())` |
| multiple returns | a struct or `std::pair` + structured bindings |
| `func(x int) int { ... }` closure | `[](int x) { ... }` lambda |
| `iota` constants | `enum class` |
| pointer parameter to mutate | `T&` parameter |
