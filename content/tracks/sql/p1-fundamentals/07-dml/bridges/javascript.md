---
from: javascript
status: draft
concept: "Creating, changing and removing stored records"
from_way: "ORM calls such as prisma.x.create, update, delete and upsert, or pg client queries."
native_way: "INSERT, UPDATE, DELETE, RETURNING and INSERT ... ON CONFLICT."
sensei_tip: "Prisma's upsert may run SELECT then INSERT; for hot counters write INSERT ... ON CONFLICT DO UPDATE yourself with $queryRaw and parameters."
---

| JavaScript | SQL |
| --- | --- |
| prisma.customer.create({ data }) | INSERT INTO customers (...) VALUES (...) RETURNING * |
| prisma.product.updateMany({ where, data }) | UPDATE products SET ... WHERE ... |
| result.rowCount | the number of rows changed |
| prisma.tag.upsert(...) | INSERT ... ON CONFLICT (name) DO UPDATE |
