---
from: javascript
status: draft
concept: "Filtering, projecting and sorting a collection"
from_way: "Array filter, map, toSorted and slice, or an ORM's where/orderBy/take options."
native_way: "WHERE, the SELECT list, ORDER BY and LIMIT, in one statement."
sensei_tip: "JavaScript's sort compares strings by UTF-16 code units; SQL's ORDER BY follows the column's collation. Do not expect identical order for accented text."
---

| JavaScript | SQL |
| --- | --- |
| arr.filter(p => p.price < 20) | WHERE price < 20 |
| arr.map(p => p.name) | SELECT name |
| toSorted((a, b) => a.price - b.price \|\| a.name.localeCompare(b.name)) | ORDER BY price, name |
| [...new Set(arr.map(p => p.category))] | SELECT DISTINCT category |
| arr.slice(0, 5) | LIMIT 5 |
