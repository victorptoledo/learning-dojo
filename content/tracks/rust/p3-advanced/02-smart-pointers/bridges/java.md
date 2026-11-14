---
from: java
status: draft
concept: "Reference counting and the heap"
from_way: "Every object is implicitly referenced and the GC manages its lifetime."
native_way: "`Box<T>` puts a value on the heap, `Rc<T>` counts references on one thread, `Arc<T>` counts them atomically across threads, `RefCell<T>` checks borrowing at runtime."
sensei_tip: "Use `Arc<Mutex<T>>` for shared mutable state across threads."
---
