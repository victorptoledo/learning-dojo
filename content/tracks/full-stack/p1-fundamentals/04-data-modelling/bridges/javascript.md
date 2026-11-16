---
from: javascript
status: reviewed
concept: "From an ORM in Node.js to SQL you understand"
from_way: "Prisma or TypeORM models, and a resolver or route that awaits a query inside a loop."
native_way: "Prisma Migrate or Knex migrations, include or joins for related rows, cursor pagination, and plain SQL with JSON aggregation where it is clearer."
sensei_tip: "await inside a for loop over rows is the most common N+1 in Node.js; look for it in every list endpoint."
---

| Node.js habit | Full stack practice |
| --- | --- |
| `for (const o of orders) await db.items.findMany(...)` | `include: { items: true }` or one join |
| `skip: 4000, take: 20` | `cursor: { id }, take: 21` |
| `prisma db push` | `prisma migrate` with reviewed SQL |
| checks in zod only | `CHECK` constraints too |
