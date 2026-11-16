---
from: go
status: reviewed
concept: "Plug-ins for the database, and sharing its connections"
from_way: "go get for a feature, and pgxpool or database/sql pools with their default sizes."
native_way: "CREATE EXTENSION for features inside the database, small pools, and PgBouncer in front of the server."
sensei_tip: "With pgx behind PgBouncer in transaction mode, use QueryExecModeSimpleProtocol or QueryExecModeExec unless PgBouncer is 1.21+ with prepared statement support."
---

| Go | PostgreSQL |
| --- | --- |
| go get a module | CREATE EXTENSION |
| pgxpool MaxConns | server connections from one binary |
| a sidecar that multiplexes connections | PgBouncer |
| conn.Exec("SET ...") on a pooled conn | session state, unsafe in transaction mode |
| a separate vector store | pgvector |
