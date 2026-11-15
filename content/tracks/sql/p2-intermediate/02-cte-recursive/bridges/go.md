---
from: go
status: draft
concept: "Named intermediate results, recursion and stored queries"
from_way: "Intermediate slices, recursive functions over a tree of structs, and helper functions that build query strings."
native_way: "WITH for steps, WITH RECURSIVE for trees, views for shared definitions."
sensei_tip: "Keep the recursive CTE in a .sql file and let sqlc or embed load it; scanning depth and path columns gives you everything needed to rebuild the tree."
---

| Go | SQL |
| --- | --- |
| paid := filter(orders, isPaid) | WITH paid AS (...) |
| func walk(n *Node, depth int) { for _, c := range n.Children { walk(c, depth+1) } } | WITH RECURSIVE ... |
| visited := map[int64]bool{} | WHERE NOT next = ANY(path) |
| a shared query constant | CREATE VIEW |
| a cache refreshed by a ticker | REFRESH MATERIALIZED VIEW on a schedule |
