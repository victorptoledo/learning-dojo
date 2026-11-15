---
from: javascript
status: draft
concept: "Specification"
from_way: "`users.filter(u => u.active && u.country === 'PT' && u.orders > 10)` copied in many places, and SQL built with template strings."
native_way: "Small predicate functions combined with helpers (`and(p, q)`), and query-builder objects (Knex, Prisma `where` objects) as specifications that become parameterised SQL."
sensei_tip: "A Prisma `where` object (`{ AND: [{ active: true }, { country: 'PT' }] }`) is a specification as data, and Prisma turns it into a parameterised query."
---

| JavaScript | Specification idea |
| --- | --- |
| `const active = u => u.active` | rule as a function |
| `const and = (p, q) => x => p(x) && q(x)` | composition |
| Prisma `where` objects | specs as query data |
| Knex `.where(...)` chains | query builder |
| template strings with values | SQL injection risk (avoid) |
