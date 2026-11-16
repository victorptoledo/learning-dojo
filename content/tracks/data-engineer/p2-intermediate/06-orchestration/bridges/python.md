---
from: python
status: draft
concept: "Scheduling and chaining jobs"
from_way: "A cron line that runs a script which calls three functions in order."
native_way: "An Airflow, Dagster or Prefect DAG whose tasks receive the logical date and can be retried and backfilled."
sensei_tip: "In Airflow, `{{ ds }}` and the data interval are the logical date; keep the code in importable modules and the DAG file thin, so tasks are testable with plain pytest."
---

| Python | Orchestration |
| --- | --- |
| cron + python job.py | time-based scheduling without state |
| @task / >> in Airflow | a task and a dependency |
| retries=3, retry_delay=... | task retries |
| ds / data_interval_start | the logical date |
| airflow dags backfill | a backfill |
