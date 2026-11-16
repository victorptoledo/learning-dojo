---
from: python
status: draft
concept: "From scripts to pipelines"
from_way: "pandas scripts in a notebook that someone runs by hand when a report is needed."
native_way: "Scheduled, tested, rerunnable pipelines whose results land in a warehouse."
sensei_tip: "A notebook is a fine place to explore; a pipeline needs a module, tests, a schedule, retries and logs. The Modern Python track covers the code side."
---

| Python | Data engineering |
| --- | --- |
| a notebook cell | an orchestrated task |
| pandas.read_csv on a laptop | an extract that lands raw files in a lake |
| df.to_csv('report.csv') | a table in a warehouse that dashboards read |
| cron + a script | a DAG with retries and alerts |
| pytest for functions | tests for data: not null, unique, relationships |
