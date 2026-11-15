---
from: javascript
status: draft
concept: "Queries inside queries, and combining whole result sets"
from_way: "Calling a function inside filter, includes on another array, and Set methods like union and difference (ES2025)."
native_way: "Subqueries and set operations executed inside the database."
sensei_tip: "arr.filter(x => other.includes(x)) is O(n·m) in memory; IN (subquery) lets the planner choose a hash or index lookup."
---

| JavaScript | SQL |
| --- | --- |
| emps.filter(e => e.salary > avgOf(e.dept)) | correlated subquery |
| ids.includes(o.customerId) | customer_id IN (SELECT ...) |
| a.union(b) | UNION |
| a.intersection(b) | INTERSECT |
| a.difference(b) | EXCEPT |
| [...a, ...b] | UNION ALL |
