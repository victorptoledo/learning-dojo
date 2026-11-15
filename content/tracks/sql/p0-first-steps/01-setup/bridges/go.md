---
from: go
status: draft
concept: "Asking a database for data instead of looping over it"
from_way: "A for loop over a slice of structs, or rows scanned one by one with database/sql."
native_way: "A SELECT that does the filtering and sorting in the database before any row reaches the program."
sensei_tip: "Go code usually writes SQL by hand (database/sql, pgx, sqlc), so fluent SQL pays off directly. sqlc even generates the Go code from your .sql files."
---

| Go | SQL |
| --- | --- |
| for _, c := range customers { if c.Country == "PT" {...} } | WHERE country = 'PT' |
| slices.SortFunc(cs, cmpByName) | ORDER BY name |
| sql.Open("pgx", dsn) | psql -d dojo |
| rows.Scan(&name, &price) | the columns of the SELECT list |
