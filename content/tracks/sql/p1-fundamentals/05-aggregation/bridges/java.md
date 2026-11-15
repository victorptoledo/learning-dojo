---
from: java
status: draft
concept: "Summarising groups of records"
from_way: "Collectors.groupingBy with counting, summingDouble or averaging, and reduce."
native_way: "GROUP BY with count, sum, avg, min and max, plus HAVING to filter the groups."
sensei_tip: "groupingBy(..., counting()) followed by a filter on the map entries is GROUP BY ... HAVING count(*) > n, computed where the data is."
---

| Java | SQL |
| --- | --- |
| groupingBy(Order::customerId, counting()) | SELECT customer_id, count(*) ... GROUP BY customer_id |
| summingBigDecimal / reducing(BigDecimal::add) | sum(total) |
| entrySet().stream().filter(e -> e.getValue() > 5) | HAVING count(*) > 5 |
| filtering(o -> o.cancelled(), counting()) | count(*) FILTER (WHERE status = 'cancelled') |
| Collectors.joining(", ") | string_agg(name, ', ') |
