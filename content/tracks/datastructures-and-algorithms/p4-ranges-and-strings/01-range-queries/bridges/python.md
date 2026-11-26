---
from: python
status: draft
concept: "Range queries"
from_way: "`sum(values[lo:hi])` or `min(values[lo:hi])`, which copies the slice and scans it every time."
native_way: "`itertools.accumulate` builds prefix sums in one line, and NumPy's `cumsum` does the same for arrays. Fenwick and segment trees are written by hand over a list; `bisect` covers sorted data."
sensei_tip: "In CPython a Fenwick tree in a plain list is usually faster than a segment tree, because it runs fewer bytecodes per operation. For heavy static workloads, NumPy `cumsum` plus two indexed reads beats any pure-Python tree."
---

| Python | Java |
| --- | --- |
| `list(accumulate(values, initial=0))` | prefix sums in a `long[]` |
| `numpy.cumsum` | `Arrays.parallelPrefix` |
| `i & -i` | `i & -i` |
| `n.bit_length() - 1` | `31 - Integer.numberOfLeadingZeros(n)` |
| a list-based Fenwick tree | a `long[]` Fenwick tree |
