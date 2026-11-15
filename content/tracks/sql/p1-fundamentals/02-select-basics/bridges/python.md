---
from: python
status: draft
concept: "Filtering, projecting and sorting a collection"
from_way: "List comprehensions, sorted with a key, set for uniqueness and slicing for the first n."
native_way: "One SELECT with WHERE, ORDER BY, DISTINCT and LIMIT."
sensei_tip: "A sort key tuple (price, name) is exactly ORDER BY price, name: both break ties with the second element."
---

| Python | SQL |
| --- | --- |
| [p.name for p in products if p.price < 20] | SELECT name FROM products WHERE price < 20 |
| sorted(ps, key=lambda p: (p.price, p.name)) | ORDER BY price, name |
| sorted(..., reverse=True) | ORDER BY ... DESC |
| set(p.category for p in ps) | SELECT DISTINCT category |
| rows[:5] | LIMIT 5 |
| 'cheap' if p < 10 else 'premium' | CASE WHEN price < 10 THEN 'cheap' ELSE 'premium' END |
