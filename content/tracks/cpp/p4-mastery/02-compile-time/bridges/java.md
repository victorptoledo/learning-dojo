---
from: java
status: draft
concept: "Compile-time programming"
from_way: "static final constants, annotation processors and code generation; almost everything else happens at run time."
native_way: "constexpr and consteval run ordinary C++ code in the compiler; static_assert checks results; type traits inspect types."
sensei_tip: "Where Java needs an annotation processor or a static initializer, C++ often needs only a constexpr function."
---

| Java | C++ |
| --- | --- |
| `static final int SIZE = 64;` | `constexpr int kSize = 64;` |
| static initializer block | a `constexpr` immediately invoked lambda |
| annotation processors | templates, `constexpr`, C++26 reflection |
| `assert` at run time | `static_assert` at compile time |
| reflection at run time | type traits now, static reflection in C++26 |
