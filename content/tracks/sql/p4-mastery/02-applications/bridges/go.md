---
from: go
status: draft
concept: "SQL behind application code"
from_way: "database/sql or pgx with $1 placeholders, sqlc, and golang-migrate."
native_way: "Bind parameters, batched queries, keyset pagination, pool settings and migrations."
sensei_tip: "fmt.Sprintf into SQL is injection; pass args to Query. pgx's pool (pgxpool) and db.SetMaxOpenConns keep the connection count bounded."
---

| Go | SQL |
| --- | --- |
| db.QueryContext(ctx, "... = $1", email) | a bind parameter |
| fmt.Sprintf("... = '%s'", email) | SQL injection |
| WHERE id = ANY($1) with a []int64 | batching instead of N+1 |
| WHERE (placed_at, id) < ($1, $2) | keyset pagination |
| pgxpool.New(ctx, dsn) | a connection pool |
| migrate -path migrations up | versioned migrations |
