---
from: java
status: draft
concept: "Bit manipulation"
from_way: "Storing flags as many boolean fields or a Set<Feature>, and writing 1 << n for long masks."
native_way: "Use EnumSet for flags of an enum (one long inside), long masks with 1L << i for ad-hoc sets, BitSet for large boolean arrays, and the Integer/Long helpers (bitCount, numberOfTrailingZeros, highestOneBit) that compile to single CPU instructions."
sensei_tip: "`EnumSet.of(READ, WRITE).contains(EXEC)` is a single AND on a long, with full type safety. And `Long.bitCount(a ^ b)` gives the Hamming distance of two 64-bit fingerprints, as SimHash-based near-duplicate detection does."
---

| Need | Java |
| --- | --- |
| a set of enum flags | `EnumSet<Feature>` |
| 64 ad-hoc flags | `long mask`, `mask \| (1L << i)` |
| a huge boolean array | `BitSet` (`set`, `get`, `and`, `or`, `cardinality`) |
| count bits | `Integer.bitCount`, `Long.bitCount` |
| lowest bit index | `Integer.numberOfTrailingZeros` |
| unsigned view | `Integer.toUnsignedLong`, `Byte.toUnsignedInt`, `>>>` |
| overflow checks | `Math.addExact`, `Math.multiplyExact` |
