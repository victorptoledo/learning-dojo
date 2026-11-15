---
from: java
status: draft
concept: "Combining related records"
from_way: "Following object references (order.getCustomer()), or nested loops and Maps keyed by id to match lists."
native_way: "JOIN ... ON: the engine matches rows with nested loops, hash or merge joins, chosen by the planner."
sensei_tip: "Lazy loading order.getCustomer() inside a loop is a nested loop over the network. Ask for the join (JOIN FETCH or a SQL join) instead."
---

| Java | SQL |
| --- | --- |
| for o : orders, for c : customers, if o.customerId == c.id | orders o JOIN customers c ON c.id = o.customer_id |
| customersById.get(o.customerId()) may be null | LEFT JOIN customers c ON ... |
| orders.stream().anyMatch(o -> o.customerId() == c.id()) | EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id) |
| noneMatch | NOT EXISTS |
| @ManyToOne + JOIN FETCH | an inner or left join |
