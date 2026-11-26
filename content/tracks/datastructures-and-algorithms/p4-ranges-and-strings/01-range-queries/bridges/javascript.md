---
from: javascript
status: draft
concept: "Range queries"
from_way: "`values.slice(lo, hi).reduce((a, b) => a + b, 0)` on every request."
native_way: "Write the structures over typed arrays (`Float64Array`, `Int32Array`, or `BigInt64Array` for large sums): they are compact and fast. JavaScript has no built-in Fenwick or segment tree."
sensei_tip: "Bitwise operators work on 32-bit integers, so `i & -i` is fine for indices but would corrupt values above 2³¹. Keep sums in a `Float64Array` (exact up to 2⁵³) or `BigInt64Array`, never in bitwise expressions."
---

| JavaScript | Java |
| --- | --- |
| `Float64Array` prefix sums | `long[]` prefix sums |
| `i & -i` | `i & -i` |
| `31 - Math.clz32(n)` | `31 - Integer.numberOfLeadingZeros(n)` |
| `BigInt64Array` | `long[]` |
| a typed-array segment tree | a `long[]` segment tree |
