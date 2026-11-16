---
from: python
status: draft
concept: "Architecture is cost: measure it, attribute it, optimise it"
from_way: "A fixed monthly VPS bill that never changes."
native_way: "Usage-based bills: tag resources, query the Cost and Usage Report, and fix the big items first."
sensei_tip: "Query CUR data with Athena from a notebook (pandas plus `awswrangler`) to find the top usage types; tuning Lambda memory with Power Tuning often lowers both cost and duration."
---

| Python world | AWS |
| --- | --- |
| a fixed VPS price | per-request and per-second billing |
| a spreadsheet of servers | Cost Explorer grouped by tag |
| Celery workers always on | Lambda or Spot workers that scale to zero |
| log files kept forever | log retention and S3 lifecycle |
