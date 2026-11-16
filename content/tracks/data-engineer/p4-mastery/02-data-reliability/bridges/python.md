---
from: python
status: draft
concept: "Running data in production"
from_way: "Rerunning a notebook when someone notices a wrong number."
native_way: "Monitored data products with SLOs, alerts on freshness and volume, and a runbook for incidents."
sensei_tip: "A small scheduled job that checks max(loaded_at) and row counts against a 4-week median and posts to chat covers most of the value before you buy a tool."
---

| Python | Data reliability |
| --- | --- |
| logging + Sentry | pipeline error alerts |
| a freshness query in a scheduled task | a freshness SLI |
| statistics.median of past counts | a volume anomaly check |
| datacompy / pandas compare | diffing old and new outputs |
| dbt build --select state:modified+ | CI for changed models |
