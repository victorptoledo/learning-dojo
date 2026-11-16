---
from: javascript
status: reviewed
concept: "Plug-ins for the database, and sharing its connections"
from_way: "npm install for a feature, and new Pool() in every serverless function instance."
native_way: "CREATE EXTENSION for features inside the database, small pools, and a pooler such as PgBouncer or the provider's pooler."
sensei_tip: "Serverless platforms multiply pools by the number of warm instances. Use the provider's pooler URL (often port 6543) and max: 1 per function."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| npm install | CREATE EXTENSION |
| new Pool({ max: 10 }) per instance | 10 server processes per instance |
| a pooled connection string from the provider | PgBouncer or Supavisor in transaction mode |
| client.query('LISTEN ...') | needs a direct, non-pooled connection |
| a vector database client | pgvector inside PostgreSQL |
