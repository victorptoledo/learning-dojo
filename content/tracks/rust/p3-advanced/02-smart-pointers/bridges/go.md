---
from: go
status: draft
concept: "Reference counting and the heap"
from_way: "Pointers to heap values managed by the GC, and sync.Mutex for shared state."
native_way: "Explicit smart pointers: Box, Rc, Arc, plus Mutex and RefCell."
sensei_tip: "A Go *T shared between goroutines is Rust's Arc<Mutex<T>>, except Rust will not compile the unsynchronised version."
---

| Go | Rust |
| --- | --- |
| &T{...} on the heap | Box::new(T { .. }) |
| pointer shared by goroutines | Arc<T> |
| sync.Mutex + pointer | Arc<Mutex<T>> |
| sync.Once | OnceLock |
| weak pointer (weak package) | Weak<T> |
