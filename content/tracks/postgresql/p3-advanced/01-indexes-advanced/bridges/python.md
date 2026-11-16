---
from: python
status: reviewed
concept: "Choosing an index structure"
from_way: "dict lookups, bisect on sorted lists, and db_index=True on Django fields."
native_way: "Six index types for six shapes of data and query, plus partial and expression indexes."
sensei_tip: "Django's Meta.indexes supports GinIndex, GistIndex, BrinIndex, conditions (partial indexes) and expressions (Index(Lower('email'), name=...)). Use AddIndexConcurrently in migrations on big tables."
---

| Python | PostgreSQL |
| --- | --- |
| bisect on a sorted list | B-tree |
| dict | Hash index |
| defaultdict(set) from word to rows | GIN |
| rtree / shapely spatial index | GiST |
| min and max per chunk | BRIN |
| GinIndex(fields=['tags']) | CREATE INDEX ... USING gin (tags) |
| Index(fields=['run_at'], condition=Q(status='pending')) | a partial index |
