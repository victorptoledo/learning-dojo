---
from: go
status: draft
concept: "A missing value that is not a value"
from_way: "Zero values instead of null, pointers, and sql.NullString or sql.Null[T] when scanning nullable columns."
native_way: "NULL as a real third state that Go has to model explicitly when it reads it."
sensei_tip: "Scanning a NULL into a plain string fails. Use *string, sql.Null[string] or coalesce(col, '') in the query when an empty value is acceptable."
---

| Go | SQL |
| --- | --- |
| var s *string; s == nil | col IS NULL |
| sql.NullInt64{Valid: false} | NULL |
| if n.Valid { ... } | CASE WHEN n IS NOT NULL THEN ... END |
| cmp.Or(nick, name) (zero value skipped) | coalesce(nick, name) |
