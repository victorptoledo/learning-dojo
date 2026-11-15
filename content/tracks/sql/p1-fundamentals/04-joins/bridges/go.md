---
from: go
status: draft
concept: "Combining related records"
from_way: "Nested loops or a map[int64]Customer built from one query and looked up while iterating another."
native_way: "A single query with JOIN, so the database matches the rows and returns them already combined."
sensei_tip: "Building a map from two queries is a hand-written hash join; it is fine for small sets, but a SQL join is one round trip and can use indexes."
---

| Go | SQL |
| --- | --- |
| for _, o := range orders { c := byID[o.CustomerID] } | JOIN customers c ON c.id = o.customer_id |
| c, ok := byID[id]; !ok | LEFT JOIN ... WHERE c.id IS NULL |
| slices.ContainsFunc(orders, ...) | EXISTS (...) |
| two nested range loops | CROSS JOIN |
