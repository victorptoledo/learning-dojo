---
from: java
status: draft
concept: "Queries inside queries, and combining whole result sets"
from_way: "A helper method called inside a stream lambda, and Set operations with addAll, retainAll and removeAll."
native_way: "Subqueries where a value, a list or a table is expected, and UNION, INTERSECT and EXCEPT on whole results."
sensei_tip: "A correlated subquery is like calling a method per element: fine for a few rows, but a grouped join is the bulk version when it gets slow."
---

| Java | SQL |
| --- | --- |
| e.salary() > avgSalaryOf(e.departmentId()) | WHERE e.salary > (SELECT avg(salary) FROM employees x WHERE x.department_id = e.department_id) |
| ids.contains(o.customerId()) | WHERE customer_id IN (SELECT id FROM ...) |
| a.addAll(b) on a Set | UNION |
| a.retainAll(b) | INTERSECT |
| a.removeAll(b) | EXCEPT |
| Stream.concat(a, b) | UNION ALL |
