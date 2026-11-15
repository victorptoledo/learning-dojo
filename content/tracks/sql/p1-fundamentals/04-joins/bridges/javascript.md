---
from: javascript
status: draft
concept: "Combining related records"
from_way: "Nested find calls, a Map from id to object, or an ORM include option."
native_way: "JOIN for matches, LEFT JOIN to keep rows without matches, EXISTS for has-one checks."
sensei_tip: "Prisma's include runs extra queries or a join behind the scenes; for reports, a single SQL join is clearer and faster."
---

| JavaScript | SQL |
| --- | --- |
| orders.map(o => ({ ...o, customer: byId.get(o.customerId) })) | orders o JOIN customers c ON c.id = o.customer_id |
| byId.get(id) ?? null | LEFT JOIN ... (NULL when missing) |
| orders.some(o => o.customerId === c.id) | EXISTS (...) |
| !orders.some(...) | NOT EXISTS (...) |
| a.flatMap(x => b.map(y => [x, y])) | CROSS JOIN |
