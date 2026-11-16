---
from: go
status: reviewed
concept: "Layered configuration"
from_way: "flag defaults, environment variables and config files merged by a library such as viper."
native_way: "Defaults, postgresql.conf, ALTER SYSTEM, database, role, session and transaction settings, later ones winning."
sensei_tip: "context.WithTimeout cancels the query from the client; statement_timeout protects the server even from clients that never cancel. Use both."
---

| Go | PostgreSQL |
| --- | --- |
| flag default | the built-in default (boot_val) |
| config file | postgresql.conf |
| env var override | ALTER SYSTEM |
| RuntimeParams in pgx config | session settings |
| context.WithTimeout | statement_timeout on the server |
