---
from: javascript
status: draft
concept: "SQL behind application code"
from_way: "node-postgres with $1 parameters, Prisma or Knex, tagged template helpers, and migration tools."
native_way: "Bind parameters, batched loading, cursor pagination and pooled connections."
sensei_tip: "Template literals are not parameters: `... = '${x}'` is injection. Use client.query(text, [x]) or Prisma's tagged $queryRaw`... ${x}`, which binds the value."
---

| JavaScript | SQL |
| --- | --- |
| client.query('... = $1', [email]) | a bind parameter |
| `... = '${email}'` | SQL injection |
| include: { author: true } | a join or batched query |
| findMany({ cursor: { id }, skip: 1, take: 20 }) | keyset pagination |
| new pg.Pool() | a connection pool |
