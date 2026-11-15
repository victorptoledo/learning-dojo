---
from: python
status: draft
concept: "Pass by reference vs aliasing"
from_way: "Every name refers to shared, mutable objects, so aliasing bugs show up at runtime."
native_way: "Borrowing: many shared &T or one mutable &mut T at a time, checked at compile time."
sensei_tip: "Changing a list while iterating over it is a runtime surprise in Python and a compile error in Rust: the loop holds a shared borrow, so a mutable one is refused."
---

| Python | Rust |
| --- | --- |
| def f(items): items.append(1) | fn f(items: &mut Vec<i32>) |
| def f(items): print(items) | fn f(items: &[i32]) |
| modifying a list while iterating | rejected by the borrow checker |
| id(a) == id(b) | std::ptr::eq(a, b) |
