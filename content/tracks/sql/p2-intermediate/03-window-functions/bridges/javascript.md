---
from: javascript
status: draft
concept: "Relational calculation without collapsing rows"
from_way: "Sorting arrays and looping with indexes to compute ranks, running sums and differences."
native_way: "Window functions over partitions, computed in the database."
sensei_tip: "A reduce that carries a running total is sum() OVER (ORDER BY ...). Doing it in SQL means pagination and filters cannot break the totals."
---

| JavaScript | SQL |
| --- | --- |
| arr.reduce((acc, x) => [...acc, (acc.at(-1) ?? 0) + x], []) | sum(x) OVER (ORDER BY day) |
| arr[i] - arr[i - 1] | x - lag(x) OVER (ORDER BY day) |
| sorted.findIndex(...) + 1 | rank() OVER (ORDER BY score DESC) |
| group then slice(0, 2) | row_number() OVER (PARTITION BY ...) <= 2 |
