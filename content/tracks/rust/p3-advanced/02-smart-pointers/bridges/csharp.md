---
from: csharp
status: draft
concept: "Reference counting and the heap"
from_way: "Reference types on the GC heap, structs on the stack or inline."
native_way: "Box, Rc, Arc and interior mutability, chosen per use."
sensei_tip: "Arc<Mutex<T>> is like a reference type whose every access goes through lock(obj). RefCell is the single-threaded version that panics on overlapping borrows."
---

| C# | Rust |
| --- | --- |
| class instance | Box<T> |
| shared reference | Rc<T> / Arc<T> |
| lock (obj) { ... } | Arc<Mutex<T>> |
| WeakReference<T> | Weak<T> |
| Lazy<T> | OnceCell / LazyLock |
