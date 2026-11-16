---
from: javascript
status: reviewed
concept: "Query execution plans"
from_way: "Chrome DevTools' performance tab, or timing a Prisma query with console.time."
native_way: "EXPLAIN (ANALYZE, BUFFERS) shows the tree of operations, with estimates, actual rows, loops and buffers."
sensei_tip: "Paste EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) output into explain.dalibo.com to see the plan as a flame-graph-like tree, like a DevTools profile."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| console.time around a query | Execution Time only |
| DevTools flame chart | the plan tree, read inside out |
| a function called in a loop | loops=N on the inner node |
| memoisation | the Memoize node |
| prisma.$queryRaw`EXPLAIN ...` | EXPLAIN from the app |
