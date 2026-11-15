---
from: python
status: draft
concept: "Access paths and the plan that uses them"
from_way: "dicts and bisect on sorted lists for fast lookups, and cProfile to find slow code."
native_way: "B-tree indexes and EXPLAIN ANALYZE."
sensei_tip: "bisect on a sorted list is what a B-tree does for ranges: it only works when the value you search is the sorted key itself, not a function of it."
---

| Python | SQL |
| --- | --- |
| bisect.bisect_left(sorted_dates, d) | an index range scan on placed_at |
| dict lookup by key | an index on the key (or a hash index) |
| sorted by (customer_id, placed_at) | a composite index |
| cProfile | EXPLAIN (ANALYZE, BUFFERS) |
| lower(email) as dict key | an expression index on lower(email) |
