---
from: javascript
status: draft
concept: "Resource Acquisition Is Initialization"
from_way: "The garbage collector frees memory; resources need try/finally or the new using declarations with Symbol.dispose."
native_way: "Objects are destroyed at the end of their scope and their destructors release resources, like using/Symbol.dispose but for everything and always."
sensei_tip: "The TC39 `using` proposal (and TypeScript 5.2's `using`) is RAII arriving in JavaScript: in C++ every local works like that."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| GC | destructors at scope exit |
| `using res = getResource()` + `[Symbol.dispose]()` | a local object + its destructor |
| `try { ... } finally { file.close() }` | `std::ofstream file(p);` |
| `new Widget()` | `std::make_unique<Widget>()` |
| `WeakRef` | `std::weak_ptr` |
| FinalizationRegistry (unpredictable) | destructors (predictable) |
