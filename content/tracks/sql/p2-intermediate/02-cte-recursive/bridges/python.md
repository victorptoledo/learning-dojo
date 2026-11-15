---
from: python
status: draft
concept: "Named intermediate results, recursion and stored queries"
from_way: "Intermediate variables, recursive functions over nested dicts, and generator functions."
native_way: "CTEs with WITH, recursive CTEs, and views."
sensei_tip: "A recursive CTE works like a breadth-first search with a queue: the anchor is the first level and each iteration processes only the newest level."
---

| Python | SQL |
| --- | --- |
| paid = [o for o in orders if o.paid] | WITH paid AS (...) |
| def walk(node, depth=0): yield node; for c in node.children: yield from walk(c, depth + 1) | WITH RECURSIVE tree AS (anchor UNION ALL step) |
| visited = set() | WHERE NOT next_id = ANY(path) |
| range(start, end) | generate_series, or a recursive CTE |
| functools.cache | a materialized view |
