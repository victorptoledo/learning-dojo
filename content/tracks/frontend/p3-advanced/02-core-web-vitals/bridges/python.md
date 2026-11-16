---
from: python
status: draft
concept: "User-facing SLOs"
from_way: "Measuring view latency with middleware and percentiles in Prometheus or an APM."
native_way: "The browser reports what the user felt: when the main content painted, how long interactions took and how much the page jumped."
sensei_tip: "Server time is only the TTFB part of LCP; profile the whole path from request to pixels."
---

| Python | In the browser |
| --- | --- |
| Request latency histogram | LCP distribution |
| cProfile hot spots | Long tasks in the Performance panel |
| Locust load test | Lab measurements |
| Sentry performance | Field RUM data |
