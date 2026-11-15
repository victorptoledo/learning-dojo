---
from: go
status: draft
concept: "Queries inside queries, and combining whole result sets"
from_way: "A helper function called in a loop, and maps used as sets for membership and differences."
native_way: "Subqueries and set operations, so the database does the matching."
sensei_tip: "Building map[int64]struct{} sets from two queries and diffing them in Go is EXCEPT with extra round trips; write the EXCEPT."
---

| Go | SQL |
| --- | --- |
| if e.Salary > avgOf(e.Dept) | correlated subquery |
| _, ok := ids[o.CustomerID] | IN (SELECT ...) |
| merge two maps | UNION |
| keys present in both maps | INTERSECT |
| keys of a not in b | EXCEPT |
| append(a, b...) | UNION ALL |
