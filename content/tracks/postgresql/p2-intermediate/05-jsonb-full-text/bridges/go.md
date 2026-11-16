---
from: go
status: reviewed
concept: "Documents and search inside the database"
from_way: "map[string]any or structs with json tags, encoding/json, and strings.Contains for search."
native_way: "jsonb with containment and jsonpath, and full-text search with tsvector, tsquery and GIN indexes."
sensei_tip: "pgx encodes a Go struct as jsonb when you pass it as a parameter, so WHERE attrs @> $1 with a filter struct is both safe and indexable."
---

| Go | PostgreSQL |
| --- | --- |
| m["specs"].(map[string]any)["weight_g"] | attrs #> '{specs,weight_g}' |
| json tags on a struct | the keys of a jsonb document |
| _, ok := m["waterproof"] | attrs ? 'waterproof' |
| strings.Contains(body, "login") | doc @@ websearch_to_tsquery('english', 'login') |
| a fuzzy-matching library | pg_trgm similarity() |
