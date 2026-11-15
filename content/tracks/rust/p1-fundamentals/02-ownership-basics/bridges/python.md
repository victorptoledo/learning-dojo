---
from: python
status: draft
concept: "Memory management without GC"
from_way: "Reference counting plus a cycle collector; names are references to shared objects."
native_way: "Each value has one owner, assignment moves it, and it is dropped when the owner goes out of scope."
sensei_tip: "In Python b = a makes two names for one list. In Rust let b = a; moves the String: a is no longer usable. Use .clone() when you really want two copies."
---

| Python | Rust |
| --- | --- |
| b = a (shared list) | let b = a; (moved) |
| copy.deepcopy(a) | a.clone() |
| with open(...) as f: | drop at the end of scope |
| del a | drop(a) |
