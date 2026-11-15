---
from: python
status: draft
concept: "Relational calculation without collapsing rows"
from_way: "pandas groupby().rank(), cumsum(), shift() and rolling(), or loops with enumerate."
native_way: "Window functions: the same operations, computed by the database over partitions."
sensei_tip: "pandas maps almost one to one: cumsum is sum() OVER (ORDER BY ...), shift(1) is lag, rolling(7).mean() is a ROWS frame, groupby().rank(method='dense') is dense_rank."
---

| Python | SQL |
| --- | --- |
| df.groupby('dept').salary.rank(ascending=False, method='min') | rank() OVER (PARTITION BY dept ORDER BY salary DESC) |
| df.revenue.cumsum() | sum(revenue) OVER (ORDER BY day) |
| df.revenue.shift(1) | lag(revenue) OVER (ORDER BY day) |
| df.revenue.rolling(7).mean() | avg(revenue) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) |
| df.groupby('dept').head(2) | row_number() ... WHERE rn <= 2 |
