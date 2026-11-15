---
from: javascript
status: draft
concept: "Object lifetime"
from_way: "The GC keeps anything reachable alive, so returning a reference to local data is always fine."
native_way: "References may never outlive their data; the compiler proves it with lifetimes."
sensei_tip: "fn longest<'a>(a: &'a str, b: &'a str) -> &'a str says: the result lives as long as both inputs. You are not choosing a lifetime, only describing the relation."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| return a substring of the input | fn f<'a>(s: &'a str) -> &'a str |
| store a reference in an object | struct Parser<'a> { input: &'a str } |
| WeakRef | Weak<T> |
| closures over variables | move closures |
