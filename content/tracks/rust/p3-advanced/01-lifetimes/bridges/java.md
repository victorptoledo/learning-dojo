---
from: java
status: draft
concept: "Object lifetime"
from_way: "The garbage collector keeps an object alive while anything references it; a reference can never dangle."
native_way: "No GC: the compiler proves at build time that each reference ends before its data is dropped. Lifetimes name those regions."
sensei_tip: "When lifetimes fight you, ask whether the struct should simply own its data. A `String` field never needs a `'a`."
---

| Java habit | Rust equivalent |
| --- | --- |
| keep a reference in a field, the GC handles it | own the value, or add a lifetime parameter |
| return a reference to a new object | return the owned value |
| share an object between threads | `Arc<T>` (owned, counted) |
| `substring` copies (since Java 7) | `&text[a..b]` borrows, no copy |
