---
from: java
status: draft
concept: "Named intermediate results, recursion and stored queries"
from_way: "Local variables for intermediate results, recursive methods walking a tree of objects, and reusable query methods."
native_way: "WITH for named steps, WITH RECURSIVE for trees and graphs, and views for queries shared by everyone."
sensei_tip: "Loading a tree with one query per level (or per node) is the N+1 problem in disguise; WITH RECURSIVE returns the whole subtree in one round trip."
---

| Java | SQL |
| --- | --- |
| var paid = orders.stream().filter(...).toList(); | WITH paid AS (SELECT ... WHERE status = 'paid') |
| void walk(Node n) { for (Node c : n.children) walk(c); } | WITH RECURSIVE ... UNION ALL ... JOIN tree ON e.manager_id = tree.id |
| Set<Node> visited | a visited path array, or the CYCLE clause |
| a repository method reused everywhere | CREATE VIEW |
| a cached computed list | CREATE MATERIALIZED VIEW ... REFRESH |
