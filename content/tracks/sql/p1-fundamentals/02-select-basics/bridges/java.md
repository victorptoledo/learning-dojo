---
from: java
status: draft
concept: "Filtering, projecting and sorting a collection"
from_way: "Streams: filter, map, sorted, distinct and limit, executed in the order you chain them."
native_way: "SELECT ... WHERE ... ORDER BY ... FETCH FIRST, evaluated in a fixed logical order whatever order you write."
sensei_tip: "Remember that WHERE runs before SELECT: an alias you define in the SELECT list does not exist yet in WHERE, unlike a local variable in a lambda chain."
---

| Java | SQL |
| --- | --- |
| .filter(p -> p.price() < 20) | WHERE price < 20 |
| .map(p -> p.name()) | SELECT name |
| .sorted(comparing(Product::price).thenComparing(Product::name)) | ORDER BY price, name |
| .distinct() | SELECT DISTINCT |
| .limit(5) | FETCH FIRST 5 ROWS ONLY (or LIMIT 5) |
| switch expression | CASE WHEN ... THEN ... ELSE ... END |
