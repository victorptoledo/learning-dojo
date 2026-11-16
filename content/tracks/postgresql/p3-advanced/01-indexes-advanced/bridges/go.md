---
from: go
status: reviewed
concept: "Choosing an index structure"
from_way: "maps, sorted slices with sort.Search, and hand-written inverted indexes."
native_way: "Six index types for six shapes of data and query, plus partial and expression indexes."
sensei_tip: "Keep index DDL in your migration files (golang-migrate, goose) and mark CREATE INDEX CONCURRENTLY migrations as non-transactional, or they fail inside the tool's transaction."
---

| Go | PostgreSQL |
| --- | --- |
| sort.Search on a sorted slice | B-tree |
| map[K]V | Hash index |
| map[string][]int64 from token to ids | GIN |
| an R-tree package | GiST |
| min and max per chunk of a time series | BRIN |
| filtering a slice before indexing it | partial index |
