---
from: go
status: draft
concept: "Where transformation runs"
from_way: "A Go binary reads rows, transforms structs in memory and writes the result to the target."
native_way: "Copy rows to raw tables quickly, and let SQL in the warehouse do the reshaping."
sensei_tip: "Go shines at fast, boring extraction (streaming rows with COPY, bounded memory). Leave business logic to versioned SQL that analysts can read."
---

| Go | ETL / ELT |
| --- | --- |
| rows.Next() scanning structs | extract |
| transforming structs | the T of ETL |
| pgx CopyFrom into raw tables | the load of ELT |
| SQL models after the copy | the T of ELT |
| embed.FS of .sql files | versioned SQL models |
