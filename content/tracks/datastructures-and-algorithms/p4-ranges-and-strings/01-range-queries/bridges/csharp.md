---
from: csharp
status: draft
concept: "Range queries"
from_way: "LINQ: `values.Skip(lo).Take(hi - lo).Sum()`, which walks the sequence from the start each time."
native_way: "Write Fenwick and segment trees over `long[]`. `Span<long>` slices without copying, and `System.Numerics.BitOperations` gives the bit helpers the structures need."
sensei_tip: "`BitOperations.Log2((uint)n)` is the exponent of the largest power of two ≤ n, which is the level a sparse-table query reads. It compiles to one CPU instruction."
---

| C# | Java |
| --- | --- |
| `values.Skip(lo).Take(len).Sum()` | a loop over the range |
| `Span<long>` | `long[]` with offsets |
| `i & -i` | `i & -i` |
| `BitOperations.Log2((uint)n)` | `31 - Integer.numberOfLeadingZeros(n)` |
| a `long[]` Fenwick tree | a `long[]` Fenwick tree |
