---
from: java
status: draft
concept: "Resource Acquisition Is Initialization"
from_way: "The garbage collector frees memory at some point; files and locks need try-with-resources or finally; finalizers are deprecated."
native_way: "Destructors run at a known point (end of scope, also during exceptions), so every resource, memory included, is owned by an object that releases it."
sensei_tip: "Every try-with-resources in Java becomes a plain local object in C++. Avoid `new` and `delete`: use std::make_unique, containers and RAII wrappers."
---

| Java | C++ |
| --- | --- |
| GC frees objects later | the destructor frees at the closing brace |
| `try (var in = new FileInputStream(p))` | `std::ifstream in(p);` (closes in its destructor) |
| `synchronized` / `lock.unlock()` in finally | `std::lock_guard lock(m);` |
| `new Widget()` (GC owned) | `auto w = std::make_unique<Widget>();` |
| shared references everywhere | `std::shared_ptr` only for real shared ownership |
| `WeakReference<T>` | `std::weak_ptr<T>` |
| memory leak = unreachable never happens | leak = a missing owner; ASan finds it |
