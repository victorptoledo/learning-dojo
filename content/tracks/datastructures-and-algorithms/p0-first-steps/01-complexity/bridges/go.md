---
from: go
status: draft
concept: "Cost of operations and amortised growth"
from_way: "Calling `append` in a loop and assuming the slice keeps sharing its array, or using `slices.Contains` inside a loop."
native_way: "A slice is a pointer, a length and a capacity over an array. `append` reuses spare capacity and otherwise allocates a larger array (about 2× for small slices, less for large ones): amortised O(1). `slices.Contains` and `slices.Index` are O(n); a `map` lookup is O(1) expected."
sensei_tip: "`make([]T, 0, n)` pre-sizes a slice, and `len` and `cap` show you the growth as it happens. Remember that after a growing `append` the new slice no longer shares memory with the old one."
---

| Go | Java | Cost |
| --- | --- | --- |
| `s = append(s, x)` | `list.add(x)` | amortised O(1) |
| `s[i]` | `list.get(i)` | O(1) |
| `cap(s)` | (internal capacity) | O(1) |
| `slices.Contains(s, x)` | `list.contains(x)` | O(n) |
| `m[k]` | `map.get(k)` | O(1) expected |
| `slices.Sort(s)` | `Arrays.sort` | O(n log n), pattern-defeating quicksort |
| `make([]T, 0, n)` | `new ArrayList<>(n)` | pre-sized, no regrowth |
