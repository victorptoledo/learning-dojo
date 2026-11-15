---
from: javascript
status: draft
concept: "Memory management without GC"
from_way: "A garbage collector; objects are shared by reference and freed when unreachable."
native_way: "Ownership: a value has one owner, moves on assignment, and is freed deterministically."
sensei_tip: "Passing an object to a function in JavaScript shares it. In Rust, passing a String by value gives it away; pass &s to lend it instead."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| const b = a (same object) | let b = a; (a moved) |
| structuredClone(a) | a.clone() |
| GC frees later | drop at the closing brace |
| f(obj) | f(&obj) to lend, f(obj) to give |
