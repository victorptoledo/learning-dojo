---
from: java
status: draft
concept: "Relational calculation without collapsing rows"
from_way: "Loading rows into the application to compute rankings, running totals or moving averages in loops."
native_way: "`ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC)` and friends compute them inside the database, row by row."
sensei_tip: "Window functions turn problems that needed many lines of backend code into one clear query, and the rows never leave the database."
---

| Java | SQL |
| --- | --- |
| a loop keeping a running sum | sum(amount) OVER (ORDER BY day) |
| groupingBy then sort each group and take 2 | row_number() OVER (PARTITION BY dept ORDER BY salary DESC) <= 2 in an outer query |
| list.get(i - 1) to compare with the previous | lag(amount) OVER (ORDER BY day) |
| a sliding window over a Deque | avg(x) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) |
| IntStream.range for ranks with ties | rank() or dense_rank() |
