---
from: java
status: draft
concept: "Memory management without GC"
from_way: "Objects allocated on the heap with `new`; a garbage collector periodically frees unreachable objects, sometimes with pauses."
native_way: "No garbage collector. Heap data such as a `String` has one owner; at the end of its scope (`}`) Rust calls `drop()` deterministically."
sensei_tip: "Passing a `String` to a function moves it: the caller loses access, and the compiler rejects any later use."
---
