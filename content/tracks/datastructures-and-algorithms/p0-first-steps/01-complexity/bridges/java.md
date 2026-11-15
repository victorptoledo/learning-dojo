---
from: java
status: draft
concept: "Cost of operations and amortised growth"
from_way: "Calling list.contains, String + and stream pipelines without thinking about what they cost inside a loop."
native_way: "Every collection call has a known cost; a loop multiplies it. ArrayList grows by 1.5× so add is amortised O(1), while contains, indexOf and remove(Object) are O(n)."
sensei_tip: "When you know the final size, `new ArrayList<>(n)` or `new HashMap<>(n * 4 / 3 + 1)` skips every intermediate resize. And `String.join` or a `StringBuilder` instead of `+=` in a loop turns O(n²) characters copied into O(n)."
---

| Java | Cost |
| --- | --- |
| `ArrayList.add(x)` | amortised O(1) (grows by 1.5×) |
| `ArrayList.get(i)` / `set(i, x)` | O(1) |
| `ArrayList.add(0, x)` / `remove(0)` | O(n): shifts everything |
| `ArrayList.contains(x)` / `indexOf(x)` | O(n) |
| `HashMap.get` / `put` | O(1) expected |
| `TreeMap.get` / `put` | O(log n) |
| `Collections.sort` / `List.sort` | O(n log n) |
| `s1 + s2` | O(len(s1) + len(s2)): a new string |
