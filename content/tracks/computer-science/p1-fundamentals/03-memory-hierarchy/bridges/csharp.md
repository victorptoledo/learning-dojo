---
from: csharp
status: draft
concept: "Cache-friendly C#"
from_way: "Using classes everywhere and ignoring layout."
native_way: "struct arrays are contiguous values; class arrays are references to separate heap objects; Span<T> and Memory<T> give views over contiguous memory without copying."
sensei_tip: "Use arrays of structs (or struct-of-arrays) in hot loops, `Span<T>` to slice without allocation, and `[StructLayout]` padding against false sharing."
---

| In C# | Underneath |
| --- | --- |
| `Point[]` with `struct Point` | contiguous values |
| `Point[]` with `class Point` | references to scattered objects |
| `Span<T>` | a pointer and a length over contiguous memory |
| `int[,]` | one contiguous block, row-major |
| `ArrayPool<T>` | reuses buffers, keeps them warm |
