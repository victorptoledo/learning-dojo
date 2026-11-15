---
from: go
status: draft
concept: "Summarising groups of records"
from_way: "A map[key]int or map[key]struct accumulated in a loop over the rows."
native_way: "GROUP BY with aggregates: one row per group comes back instead of every row."
sensei_tip: "Accumulating maps in Go means reading every row over the network; let GROUP BY do it and scan the few summary rows."
---

| Go | SQL |
| --- | --- |
| counts[o.CustomerID]++ | count(*) ... GROUP BY customer_id |
| totals[o.CustomerID] += o.Total | sum(total) |
| if counts[k] > 5 { ... } | HAVING count(*) > 5 |
| strings.Join(names, ", ") | string_agg(name, ', ') |
