---
from: python
status: draft
concept: "Combining related records"
from_way: "Dictionaries keyed by id, nested comprehensions, or pandas merge with how='inner' or 'left'."
native_way: "JOIN, LEFT JOIN, FULL JOIN and EXISTS, executed by the database."
sensei_tip: "pandas merge(how='left') is LEFT JOIN, how='outer' is FULL JOIN, and indicator=True plus a filter is the anti join. The fan-out trap is the same in both."
---

| Python | SQL |
| --- | --- |
| df.merge(orders, left_on='id', right_on='customer_id') | JOIN orders o ON o.customer_id = c.id |
| merge(how='left') | LEFT JOIN |
| merge(how='outer') | FULL JOIN |
| any(o.customer_id == c.id for o in orders) | EXISTS (...) |
| itertools.product(a, b) | CROSS JOIN |
