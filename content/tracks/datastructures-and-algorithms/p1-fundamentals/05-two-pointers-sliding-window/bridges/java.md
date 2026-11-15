---
from: java
status: draft
concept: "Two pointers and sliding windows"
from_way: "Two nested for loops over every (i, j) pair, or streams that rebuild a sublist for every window."
native_way: "Keep two int indexes and a small piece of state (a running sum, a map of counts, a deque) and move each index forward only. The whole scan is O(n) and allocates almost nothing."
sensei_tip: "Use `long` for running sums and `Map.merge(key, 1, Integer::sum)` for counts. `HashMap.getOrDefault(prefix - k, 0)` is the whole trick of counting subarrays with sum k."
---

| Pattern | Java building blocks |
| --- | --- |
| opposite-end pointers | `int i = 0, j = a.length - 1; while (i < j)` |
| read/write pointers | `for (int r = 0; r < n; r++) if (keep) a[w++] = a[r];` |
| window counts | `HashMap<Character, Integer>` or `int[128]` |
| window max/min | `ArrayDeque<Integer>` of indexes |
| prefix sums + counts | `HashMap<Long, Integer>`, `merge`, `getOrDefault` |
| many range queries | `long[] prefix = new long[n + 1]` |
