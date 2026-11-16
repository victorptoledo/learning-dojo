---
from: python
status: draft
concept: "Metrics, logs and traces tell you what you cannot see"
from_way: "The `logging` module prints text, and Sentry catches exceptions."
native_way: "Structured JSON logs in CloudWatch Logs, metrics from EMF, traces in X-Ray, alarms on error rate and latency."
sensei_tip: "Powertools' `Logger` adds the request id and cold-start flag to every JSON line; `Metrics` writes EMF so custom metrics cost no API calls."
---

| Python world | AWS |
| --- | --- |
| `logging.info("...")` | a JSON log line with level and request id |
| statsd counters | EMF custom metrics |
| grep on a log file | Logs Insights queries |
| Sentry alerts | alarms on Errors and metric filters |
