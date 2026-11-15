---
from: python
status: draft
concept: "Object lifetime"
from_way: "A garbage collector keeps objects alive as long as anything references them."
native_way: "Lifetimes describe, at compile time, how long a reference is valid; nothing is kept alive for you."
sensei_tip: "There is no Python equivalent because the GC hides the question. A lifetime error means: this reference could outlive the value it points to. Often the fix is to return an owned value."
---

| Python | Rust |
| --- | --- |
| return part of a local list | return an owned String, or borrow from the input |
| closures capturing variables | move closures |
| weakref | Weak<T> |
| objects stored in a cache | owned values or Rc/Arc |
