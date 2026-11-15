---
from: python
status: draft
concept: "Queries that look right and are not"
from_way: "Pythonic pitfalls: mutable defaults, float rounding, is versus ==, and the ORM's lazy loading."
native_way: "SQL's pitfalls: NULL, fan-out, functions on indexed columns, OFFSET and BETWEEN on timestamps."
sensei_tip: "Django's .count() and len(queryset) and .exists() do very different amounts of work; .exists() is EXISTS. Prefer it for has-one checks."
---

| Python | SQL |
| --- | --- |
| len(qs) > 0 | count(*) or EXISTS: prefer EXISTS |
| float for prices | numeric |
| x == None | x IS NULL (= NULL never matches) |
| date == day on a datetime | a half-open range on the timestamp |
| set() to dedupe join results | fix the join instead |
