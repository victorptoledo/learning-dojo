---
from: javascript
status: draft
concept: "Generic code"
from_way: "TypeScript generics and constraints with extends exist only for type checking; the JavaScript code is the same for every type."
native_way: "Templates produce a specialised copy of the code per type; concepts check constraints; nothing is erased."
sensei_tip: "`function max<T extends { size: number }>(a: T, b: T)` becomes a template constrained by a concept with a requires-expression."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `function id<T>(x: T): T` | `template <typename T> T id(T x)` |
| `T extends Shape` | `template <Shape T>` (a concept) |
| structural typing | requires-expressions check structure |
| conditional types | `std::conditional_t`, `if constexpr` |
| generics erased in JS output | a compiled function per type |
| `...args: T[]` | parameter packs |
