---
from: csharp
status: draft
concept: "Memory management without GC"
from_way: "The .NET garbage collector, IDisposable and using for resources."
native_way: "Ownership replaces both the GC and IDisposable: drop runs when the owner goes out of scope."
sensei_tip: "Drop is like Dispose, but the compiler calls it for every value, in reverse order of creation. There is no finaliser thread and no forgotten using."
---

| C# | Rust |
| --- | --- |
| var b = a; (shared) | let b = a; (moved) |
| a.Clone() | a.clone() |
| using var f = File.Open(...) | let f = File::open(...)?; (closed on drop) |
| IDisposable.Dispose | impl Drop |
