---
from: javascript
status: draft
concept: "Dynamic programming"
from_way: "Memoising with an object keyed by template strings like `${i},${j}`."
native_way: "Use typed arrays for numeric tables (Int32Array, Float64Array) and a Map for sparse states. Numbers are doubles, so counts above 2^53 lose precision: use BigInt or a modulus."
sensei_tip: "A 2D table as one flat typed array, `dp[i * (m + 1) + j]`, is faster and lighter than an array of arrays, and `Array.from({ length: n }, () => new Int32Array(m))` builds the nested form without shared rows."
---

| JavaScript | Java |
| --- | --- |
| `new Float64Array(n + 1)` | `long[]` / `double[]` |
| flat `dp[i * (m + 1) + j]` | `int[][] dp` |
| `Map` with string keys | `HashMap<record, Long>` |
| `BigInt` | `BigInteger` / modular `long` |
| `Infinity` | `Integer.MAX_VALUE` marker |
