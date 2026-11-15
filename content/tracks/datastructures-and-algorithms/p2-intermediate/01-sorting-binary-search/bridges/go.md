---
from: go
status: draft
concept: "Sorting and searching"
from_way: "Implementing sort.Interface (Len, Less, Swap) for every type."
native_way: "slices.Sort and slices.SortFunc (pattern-defeating quicksort, not stable) and slices.SortStableFunc for stable sorts; slices.BinarySearch returns the insertion point and whether the value was found; sort.Search runs a binary search on any predicate."
sensei_tip: "`sort.Search(n, func(i int) bool { return can(i) })` is binary search on the answer in one line: it returns the smallest i in [0, n) for which the predicate is true, assuming it is monotonic. `cmp.Compare(a, b)` is the overflow-free comparison."
---

| Go | Java |
| --- | --- |
| `slices.Sort(s)` | `Arrays.sort` |
| `slices.SortFunc(s, func(a, b T) int { return cmp.Compare(a.X, b.X) })` | `list.sort(comparing(T::x))` |
| `slices.SortStableFunc` | `List.sort` (stable) |
| `i, found := slices.BinarySearch(s, x)` | `Arrays.binarySearch` |
| `sort.Search(n, pred)` | binary search on the answer |
| `cmp.Compare(a, b)` | `Integer.compare(a, b)` |
