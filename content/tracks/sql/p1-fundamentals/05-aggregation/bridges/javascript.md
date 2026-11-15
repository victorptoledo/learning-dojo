---
from: javascript
status: draft
concept: "Summarising groups of records"
from_way: "reduce into an object, Object.groupBy (ES2024), or lodash groupBy and sumBy."
native_way: "GROUP BY with aggregates computed by the database in one pass."
sensei_tip: "Object.groupBy builds the groups in memory after transferring every row; GROUP BY transfers only one row per group."
---

| JavaScript | SQL |
| --- | --- |
| Object.groupBy(orders, o => o.customerId) | GROUP BY customer_id |
| arr.reduce((s, o) => s + o.total, 0) | sum(total) (exact with numeric) |
| Object.entries(g).filter(([, v]) => v.length > 5) | HAVING count(*) > 5 |
| names.join(', ') | string_agg(name, ', ') |
