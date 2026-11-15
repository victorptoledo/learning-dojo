---
from: python
status: draft
concept: "Reference counting and the heap"
from_way: "Every object lives on the heap and is shared by reference counting."
native_way: "Box for the heap, Rc and Arc for shared ownership, RefCell and Mutex for mutation through shared references."
sensei_tip: "Rc<RefCell<T>> is how Rust writes what Python does by default: a shared, mutable object. It is rarely needed; first try to give the data one owner."
---

| Python | Rust |
| --- | --- |
| any object | Box<T> when it must be on the heap |
| sys.getrefcount | Rc::strong_count |
| shared mutable object | Rc<RefCell<T>> |
| weakref.ref | Weak<T> |
| threads sharing an object | Arc<Mutex<T>> |
