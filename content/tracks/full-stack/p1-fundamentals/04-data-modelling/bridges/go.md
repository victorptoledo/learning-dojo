---
from: go
status: reviewed
concept: "database/sql and explicit queries"
from_way: "Go code with hand-written queries is explicit, but it is easy to call a query per row in a loop."
native_way: "Migrations with golang-migrate or goose, sqlc to generate typed code from SQL, joins or ANY($1) batches for related rows, and keyset paging."
sensei_tip: "sqlc keeps SQL in .sql files and generates Go from it: the queries stay reviewable and the types stay right."
---

| Go habit | Full stack practice |
| --- | --- |
| `db.Query` inside a `for` loop | one `WHERE order_id = ANY($1)` |
| `LIMIT $1 OFFSET $2` | `WHERE (placed_at, id) < ($1, $2)` |
| schema in an init function | golang-migrate or goose migrations |
| validation in handlers | constraints in the schema too |
