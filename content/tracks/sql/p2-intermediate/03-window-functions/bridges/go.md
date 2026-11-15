---
from: go
status: draft
concept: "Relational calculation without collapsing rows"
from_way: "Sorting slices and looping with indexes to compute running totals, ranks and differences."
native_way: "Window functions, so the rows arrive with the totals and ranks already computed."
sensei_tip: "Rows come out of rows.Next() in ORDER BY order; with window functions each row already carries its running total, so the Go loop only scans."
---

| Go | SQL |
| --- | --- |
| sum += r.Revenue; r.Running = sum | sum(revenue) OVER (ORDER BY day) |
| rows[i].Revenue - rows[i-1].Revenue | revenue - lag(revenue) OVER (ORDER BY day) |
| rank := i + 1 after slices.SortFunc | rank() OVER (ORDER BY ...) |
| a map of department to the top two | row_number() OVER (PARTITION BY dept ...) <= 2 |
