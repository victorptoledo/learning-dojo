---
from: javascript
status: reviewed
concept: "What sits behind a database connection"
from_way: "new pg.Pool() in Node, or a Prisma client, where a connection looks like a cheap object."
native_way: "Each connection is a server process with its own memory; the catalogs describe every object."
sensei_tip: "Serverless functions that each open a pool can exhaust max_connections in minutes. Put PgBouncer or your provider's pooler in front and keep max per instance small."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| new Pool({ max: 10 }) | up to 10 backend processes |
| prisma db pull | reads pg_catalog to build the schema |
| client.query('SELECT pg_backend_pid()') | the pid of your backend |
| DATABASE_URL=postgres://.../shop | one database per connection |
| a lambda cold start with a new pool | new processes forked on the server |
