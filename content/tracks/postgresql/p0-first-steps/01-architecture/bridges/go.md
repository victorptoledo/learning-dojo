---
from: go
status: reviewed
concept: "What sits behind a database connection"
from_way: "database/sql or pgxpool, where *sql.DB is a pool and goroutines are cheap."
native_way: "Goroutines are cheap, but each server connection is a process with its own memory."
sensei_tip: "Always call db.SetMaxOpenConns (or set pgxpool's MaxConns): database/sql defaults to unlimited, and a burst of goroutines becomes a burst of server processes."
---

| Go | PostgreSQL |
| --- | --- |
| pgxpool.New(ctx, url) | a pool of backend processes |
| db.SetMaxOpenConns(20) | at most 20 processes from this binary |
| conn.PgConn().PID() | pg_backend_pid() |
| search_path in the connection string | SET search_path |
| sqlc reading the schema | the same catalogs psql reads |
