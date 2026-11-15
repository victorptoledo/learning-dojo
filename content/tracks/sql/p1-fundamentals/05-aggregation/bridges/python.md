---
from: python
status: draft
concept: "Summarising groups of records"
from_way: "collections.Counter, itertools.groupby on sorted data, or pandas groupby().agg()."
native_way: "GROUP BY with aggregate functions, HAVING and FILTER."
sensei_tip: "df.groupby('customer_id').agg(orders=('id', 'count'), revenue=('total', 'sum')) is a GROUP BY; .query('orders > 5') afterwards is HAVING."
---

| Python | SQL |
| --- | --- |
| Counter(o.customer_id for o in orders) | SELECT customer_id, count(*) ... GROUP BY customer_id |
| sum(o.total for o in orders) | sum(total) |
| {k: v for k, v in c.items() if v > 5} | HAVING count(*) > 5 |
| ', '.join(names) | string_agg(name, ', ') |
| df.pivot_table(...) | conditional aggregates, one per column |
