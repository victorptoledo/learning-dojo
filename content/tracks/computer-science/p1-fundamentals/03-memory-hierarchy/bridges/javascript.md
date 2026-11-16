---
from: javascript
status: draft
concept: "Cache-friendly JavaScript"
from_way: "Treating all arrays alike."
native_way: "V8 stores arrays of small integers or doubles unboxed and contiguously (packed elements); mixing types or creating holes falls back to slower, pointer-based layouts; typed arrays are always contiguous."
sensei_tip: "Use Float64Array or Int32Array for numeric work, keep arrays packed (no holes, one element type), and create objects with the same shape."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `[1, 2, 3]` | packed SMI elements |
| `arr[1000] = x` on a short array | holes: a slower layout |
| `new Float64Array(n)` | contiguous doubles |
| objects with the same shape | shared hidden class, predictable field offsets |
| `SharedArrayBuffer` | memory shared between workers (watch false sharing) |
