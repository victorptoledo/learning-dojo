---
from: go
status: draft
concept: "Range queries"
from_way: "A `for _, v := range values[lo:hi]` loop for every question."
native_way: "Write the structures over `[]int64`. Slicing is free (no copy), and `math/bits` has `Len`, `TrailingZeros` and `OnesCount` for the index arithmetic."
sensei_tip: "`bits.Len(uint(n)) - 1` is the exponent of the largest power of two ≤ n. Go integers do not overflow with an error, they wrap, so use `int64` for sums of large counts."
---

| Go | Java |
| --- | --- |
| `[]int64` | `long[]` |
| `values[lo:hi]` (a view) | `Arrays.copyOfRange` (a copy) |
| `i & -i` | `i & -i` |
| `bits.Len(uint(n)) - 1` | `31 - Integer.numberOfLeadingZeros(n)` |
| a slice-based segment tree | a `long[]` segment tree |
