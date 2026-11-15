---
from: java
status: draft
concept: "Dynamic programming"
from_way: "Memoising with HashMap<String, Long> and keys built by string concatenation, or recursing so deep that the stack overflows."
native_way: "Use primitive arrays (long[], int[][]) for integer states and a HashMap keyed by a record for others; fill tables bottom-up for large inputs so no recursion is needed, and roll rows to keep memory O(width)."
sensei_tip: "`Map.computeIfAbsent` must not be used for recursive memoisation: modifying a HashMap from inside its own computeIfAbsent throws ConcurrentModificationException since Java 9. Use `get`, compute, then `put`."
---

| Need | Java |
| --- | --- |
| 1D table | `long[] dp = new long[n + 1]` |
| 2D table | `int[][] dp = new int[n + 1][m + 1]` |
| rolling rows | two `int[]` swapped each iteration |
| unreachable marker | `Integer.MAX_VALUE` checked before adding |
| sparse state memo | `HashMap<State, Long>` with `record State(int i, int mask)` |
| modular counts | `% 1_000_000_007` on every addition |
| binary search inside DP | `Arrays.binarySearch` or a hand-written lowerBound |
