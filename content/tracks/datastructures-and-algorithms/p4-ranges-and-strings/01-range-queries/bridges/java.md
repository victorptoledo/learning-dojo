---
from: java
status: draft
concept: "Range queries"
from_way: "A loop over `list.subList(lo, hi)` for every question, or `IntStream.range(lo, hi).map(...).sum()`."
native_way: "The JDK has no Fenwick or segment tree: write them over `long[]`. For static sums, `Arrays.parallelPrefix(values, Long::sum)` builds prefix sums in place; `TreeMap` covers ordered keys but not range aggregates."
sensei_tip: "Keep the arrays primitive (`long[]`, not `List<Long>`): a segment tree of boxed values is several times larger and slower, and `Math.min` on `long` avoids unboxing in the inner loop."
---

| Need | Java |
| --- | --- |
| prefix sums | `Arrays.parallelPrefix(prefix, Long::sum)` or a loop |
| lowest set bit | `i & -i`, or `Integer.lowestOneBit(i)` |
| largest power of two ≤ n | `Integer.highestOneBit(n)`, `31 - Integer.numberOfLeadingZeros(n)` for its exponent |
| Fenwick / segment tree | your own class over `long[]` |
| ordered keys with ranges | `TreeMap.subMap(lo, hi)` (no aggregates) |
