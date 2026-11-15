---
from: go
status: draft
concept: "Thinking in tables"
from_way: "SQL written by hand, often with sqlc or migrations in SQL files."
native_way: "Good schema design to go with the hand-written SQL."
sensei_tip: "Writing SQL directly makes indexes and constraints visible; keep them in migrations next to the queries that rely on them."
---

| Go | In practice |
| --- | --- |
| CREATE TABLE in migrations | tables with keys and constraints |
| sqlc queries | queries that need matching indexes |
| struct tags | column names |
| errors on duplicate keys | UNIQUE constraints doing their job |
