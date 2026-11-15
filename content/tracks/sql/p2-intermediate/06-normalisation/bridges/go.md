---
from: go
status: draft
concept: "Storing each fact once"
from_way: "Structs with slices of child structs and IDs pointing to other structs."
native_way: "Tables per entity, child rows in their own tables with foreign keys."
sensei_tip: "A []Tag field in a struct becomes a separate table plus a junction table; load it with a second query using = ANY($1) of the parent ids."
---

| Go | SQL |
| --- | --- |
| type Post struct { Tags []string } | post_tags (post_id, tag_id) |
| CustomerID int64 field | a foreign key |
| duplicated struct fields | a dependency that wants its own table |
| a cached counter field | deliberate denormalisation kept by the same transaction |
