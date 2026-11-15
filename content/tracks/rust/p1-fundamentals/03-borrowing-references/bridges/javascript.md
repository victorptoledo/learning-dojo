---
from: javascript
status: draft
concept: "Pass by reference vs aliasing"
from_way: "Objects passed by reference and changeable by anyone who holds them."
native_way: "Shared borrows for reading, one exclusive borrow for writing."
sensei_tip: "A function receiving &mut Vec in Rust is like getting an array in JavaScript that nobody else may touch until you return. The type tells callers what you will do with their data."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| function f(arr) { arr.push(1) } | fn f(arr: &mut Vec<i32>) |
| function f(arr) { return arr.length } | fn f(arr: &[i32]) -> usize |
| two references mutating one object | a compile error |
| readonly T[] in TypeScript | &[T] |
