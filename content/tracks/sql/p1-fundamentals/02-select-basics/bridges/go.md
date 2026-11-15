---
from: go
status: draft
concept: "Filtering, projecting and sorting a collection"
from_way: "A loop with an if to filter, slices.SortFunc to sort and a slice expression to keep the first n."
native_way: "WHERE, ORDER BY and LIMIT, done before the rows reach rows.Next()."
sensei_tip: "Filter and sort in SQL, not in the loop over rows.Next(): you transfer fewer rows and the database can use an index."
---

| Go | SQL |
| --- | --- |
| if p.Price < 20 { out = append(out, p) } | WHERE price < 20 |
| slices.SortFunc(ps, func(a, b P) int { return cmp.Or(cmp.Compare(a.Price, b.Price), strings.Compare(a.Name, b.Name)) }) | ORDER BY price, name |
| ps[:5] | LIMIT 5 |
| map[string]struct{} for unique values | SELECT DISTINCT |
| switch { case p < 10: ... } | CASE WHEN ... END |
