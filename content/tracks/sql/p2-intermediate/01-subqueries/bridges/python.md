---
from: python
status: draft
concept: "Queries inside queries, and combining whole result sets"
from_way: "Nested comprehensions, any() over a sub-list, and set operators |, & and -."
native_way: "Subqueries in WHERE, SELECT and FROM, and set operations on results."
sensei_tip: "set(a) | set(b) is UNION, & is INTERSECT, - is EXCEPT; a list concatenation a + b is UNION ALL. SQL's versions compare whole rows."
---

| Python | SQL |
| --- | --- |
| [e for e in emps if e.salary > avg_of(e.dept)] | correlated subquery in WHERE |
| any(o.customer_id == c.id for o in orders) | EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id) |
| set(a) \| set(b) | UNION |
| set(a) & set(b) | INTERSECT |
| set(a) - set(b) | EXCEPT |
| a + b | UNION ALL |
