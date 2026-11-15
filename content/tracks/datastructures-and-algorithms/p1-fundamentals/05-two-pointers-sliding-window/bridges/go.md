---
from: go
status: draft
concept: "Two pointers and sliding windows"
from_way: "Re-slicing `a[i:i+k]` and summing it for every window."
native_way: "Two indexes and a running aggregate; re-slicing is free (no copy) in Go, but summing a slice is still O(k). Maps and fixed arrays (`[128]int`) hold the window counts."
sensei_tip: "A `[128]int` (or `[256]int` for bytes) is a value type on the stack: the fastest possible counter for a window over ASCII. Range over a string with `for i, r := range s` if the text may contain multi-byte runes."
---

| Pattern | Go | Java |
| --- | --- | --- |
| opposite ends | `i, j := 0, len(a)-1` | `int i = 0, j = n - 1` |
| window counts | `[128]int` or `map[rune]int` | `int[128]` or `HashMap` |
| window max | slice used as a deque | `ArrayDeque` |
| prefix sums | `prefix := make([]int64, n+1)` | `long[] prefix` |
| count by prefix | `seen[prefix-k]` (zero value if absent) | `getOrDefault(prefix - k, 0)` |
