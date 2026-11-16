---
from: java
status: draft
concept: "Functions and parameters"
from_way: "Methods live in classes; every object argument is a reference passed by value; Optional<T> for maybe-results; lambdas capture effectively final variables."
native_way: "Free functions are normal; you choose by value, by const& or by & for each parameter; std::optional holds the value inline; lambdas choose copy or reference captures."
sensei_tip: "Passing `const T&` is the C++ equivalent of passing a Java object without letting the callee modify it; plain `T` really copies."
---

| Java | C++ |
| --- | --- |
| `static int area(int w, int h)` in a class | `int area(int w, int h)` in a namespace |
| object parameter (shared) | `const T&` (read) or `T&` (modify) |
| `Optional<User>` | `std::optional<User>` |
| `x -> x * 2` | `[](int x) { return x * 2; }` |
| effectively final capture | `[x]` copy or `[&x]` reference |
| `enum Level { INFO }` | `enum class Level { Info }` |
| records / `var (a, b)`? | structured bindings `auto [a, b] = f();` |
