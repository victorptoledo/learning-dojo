---
from: javascript
status: draft
concept: "Named intermediate results, recursion and stored queries"
from_way: "const intermediate arrays, recursive functions over children arrays, and memoised selectors."
native_way: "WITH for named steps, WITH RECURSIVE for hierarchies, views to share a definition."
sensei_tip: "Fetching a category tree level by level from the API is slow; one recursive CTE returns every node with its depth and path, ready to nest in JavaScript."
---

| JavaScript | SQL |
| --- | --- |
| const paid = orders.filter(o => o.paid) | WITH paid AS (...) |
| function walk(n, d = 0) { n.children.forEach(c => walk(c, d + 1)) } | WITH RECURSIVE ... |
| const seen = new Set() | a visited path array or CYCLE |
| a reusable query function | CREATE VIEW |
| a cached selector (reselect) | a materialized view |
