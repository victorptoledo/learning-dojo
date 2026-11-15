---
from: javascript
status: draft
concept: "Asking a database for data instead of looping over it"
from_way: "Array methods like filter and map over JSON from an API, or an ORM call such as Prisma's findMany."
native_way: "A SELECT that names the result; the engine plans how to get it."
sensei_tip: "Behind every findMany there is SQL. Learning it lets you read what Prisma, Sequelize or Knex send, and write the queries they cannot."
---

| JavaScript | SQL |
| --- | --- |
| customers.filter(c => c.country === 'PT') | SELECT * FROM customers WHERE country = 'PT' |
| arr.toSorted((a, b) => a.name.localeCompare(b.name)) | ORDER BY name |
| new pg.Client().connect() | psql -d dojo |
| prisma.product.findMany({ where: { price: { lt: 20 } } }) | SELECT * FROM products WHERE price < 20 |
