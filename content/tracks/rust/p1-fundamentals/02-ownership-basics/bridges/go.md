---
from: go
status: draft
concept: "Memory management without GC"
from_way: "A garbage collector and escape analysis; defer for cleanup."
native_way: "No garbage collector: each value has an owner and is dropped at the end of its scope."
sensei_tip: "Go's defer f.Close() becomes unnecessary: a File closes itself when dropped. Moving a value is free; clone() is the explicit, visible cost."
---

| Go | Rust |
| --- | --- |
| b := a (copies the header, shares data) | let b = a; (moved) |
| copy(dst, src) | a.clone() |
| defer f.Close() | drop at end of scope |
| GC pauses | deterministic frees |
