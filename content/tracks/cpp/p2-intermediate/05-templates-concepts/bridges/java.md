---
from: java
status: draft
concept: "Generics and constraints"
from_way: "Generics are erased at run time, work only with reference types, and are bounded with extends and super."
native_way: "Templates generate code per type at compile time, work with any type including int, and are constrained with concepts."
sensei_tip: "`<T extends Comparable<T>>` becomes `template <std::totally_ordered T>`: the constraint is checked, and there is no boxing or erasure."
---

| Java | C++ |
| --- | --- |
| `List<Integer>` (boxed) | `std::vector<int>` (no boxing) |
| `<T extends Comparable<T>>` | `template <std::totally_ordered T>` |
| type erasure at run time | a separate instantiation per type |
| `new T()` impossible | `T{}` works |
| interface as a bound | a concept with a requires-expression |
| generics in .class files | template definitions in headers |
