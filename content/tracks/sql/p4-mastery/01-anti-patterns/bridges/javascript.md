---
from: javascript
status: draft
concept: "Queries that look right and are not"
from_way: "Pitfalls like == coercion, floating point money and async loops with await inside."
native_way: "SQL pitfalls with the same flavour: implicit casts, float money, row-by-row loops."
sensei_tip: "for (const id of ids) await db.query(...) is RBAR plus N+1. Send the whole array: WHERE id = ANY($1)."
---

| JavaScript | SQL |
| --- | --- |
| 0.1 + 0.2 !== 0.3 | use numeric, not double precision |
| '5' == 5 coercion | implicit casts that disable indexes |
| await in a for loop of queries | one set-based statement |
| offset pagination in the API | keyset pagination |
