---
from: javascript
status: draft
concept: "Repository and unit of work"
from_way: "Route handlers that run queries with the database client and build SQL strings inline."
native_way: "Repository modules or classes per aggregate over an ORM or query builder (Prisma, TypeORM, Knex), with the transaction passed explicitly to each repository call."
sensei_tip: "Most Node ORMs have no implicit transaction context: `prisma.$transaction(async (tx) => ...)` gives you a transaction client, and every repository call in the use case must use `tx`, not the global client."
---

| JavaScript | Pattern |
| --- | --- |
| `accountsRepo.find(id)` modules | repository |
| `prisma.$transaction(fn)` | unit of work boundary |
| TypeORM `EntityManager` | unit of work, identity map |
| Sequelize `Model.save()` | active record |
| an object with a `Map` inside | in-memory repository for tests |
