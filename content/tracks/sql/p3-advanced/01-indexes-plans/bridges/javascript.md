---
from: javascript
status: draft
concept: "Access paths and the plan that uses them"
from_way: "A Map keyed by id, sorted arrays with binary search, and the browser's performance panel."
native_way: "Indexes on the columns you filter and sort on, verified with EXPLAIN."
sensei_tip: "Prisma's @@index([customerId, placedAt]) creates exactly the composite index of this lesson; check it is used with EXPLAIN in a SQL console."
---

| JavaScript | SQL |
| --- | --- |
| new Map(rows.map(r => [r.id, r])) | the primary key index |
| @@index([customerId, placedAt]) in schema.prisma | CREATE INDEX ON orders (customer_id, placed_at) |
| date.toISOString().slice(0, 10) === day | non-sargable: use a range instead |
| performance.now() around a call | EXPLAIN ANALYZE |
