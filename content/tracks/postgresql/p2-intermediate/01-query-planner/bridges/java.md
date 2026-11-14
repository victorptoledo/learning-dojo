---
from: java
status: draft
concept: "Query execution plans"
from_way: "Reading ORM logs without knowing why a query locks up the database in production."
native_way: "`EXPLAIN ANALYZE` runs the query and shows the real time spent in every node of the plan."
sensei_tip: "Watch 'shared hit' vs 'shared read' in `BUFFERS`: reads outside the buffer cache are much slower."
---
