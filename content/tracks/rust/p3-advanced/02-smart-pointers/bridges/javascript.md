---
from: javascript
status: draft
concept: "Reference counting and the heap"
from_way: "All objects are heap references managed by the GC."
native_way: "Heap allocation and sharing are explicit with Box, Rc and Arc."
sensei_tip: "A tree with parent links is easy in JavaScript and needs Rc plus Weak in Rust (or an arena with indices). Arenas are often simpler and faster."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| new Node() | Box::new(Node { .. }) |
| shared object | Rc<T> |
| mutable shared object | Rc<RefCell<T>> |
| WeakRef | Weak<T> |
| SharedArrayBuffer between workers | Arc<T> |
