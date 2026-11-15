---
from: go
status: draft
concept: "Dynamic programming"
from_way: "Allocating a new 2D slice inside every loop iteration."
native_way: "Allocate tables once with make, often as one flat []int64 indexed by i*(m+1)+j; use a map with a struct key for sparse states. Go's growable stacks make deep top-down recursion safer than in Java, but bottom-up loops remain faster."
sensei_tip: "`dp := make([]int64, (n+1)*(m+1))` is one allocation instead of n+1; and `min`/`max` are built-in functions since Go 1.21, which keeps recurrences short."
---

| Go | Java |
| --- | --- |
| `make([]int64, n+1)` | `new long[n + 1]` |
| flat `make([]int, (n+1)*(m+1))` | `int[][]` |
| `map[struct{ i, j int }]int64` | `HashMap<record, Long>` |
| built-in `min`, `max` | `Math.min`, `Math.max` |
| `math.MaxInt` marker | `Integer.MAX_VALUE` |
