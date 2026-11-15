---
from: python
status: draft
concept: "Reliability as a number"
from_way: "Logs and some dashboards, with alerts on CPU and errors."
native_way: "SLIs, SLOs and error budgets, with alerts on user-facing symptoms."
sensei_tip: "Export request duration as a histogram with prometheus_client; SLOs are computed from those buckets, not from averages."
---

| Python | In practice |
| --- | --- |
| print/logging | structured logs with request ids |
| prometheus_client Histogram | latency SLI |
| alert on CPU | alert on error budget burn |
| Sentry | error tracking, one signal among several |
