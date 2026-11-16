---
from: javascript
status: reviewed
concept: "Layered configuration"
from_way: "dotenv files, process.env and config libraries that merge defaults with per-environment files."
native_way: "Defaults, postgresql.conf, ALTER SYSTEM, database, role, session and transaction settings, later ones winning."
sensei_tip: "node-postgres accepts statement_timeout in the Pool config, but ALTER ROLE api SET statement_timeout covers every client, including the one you forgot."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| config/default.json | postgresql.conf |
| process.env.X | ALTER SYSTEM / command line |
| config/production.json | ALTER DATABASE / ALTER ROLE ... SET |
| new Pool({ statement_timeout: 30000 }) | a session setting |
| a per-request override | SET LOCAL in a transaction |
