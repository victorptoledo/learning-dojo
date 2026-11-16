---
from: java
status: draft
concept: "User-facing SLOs"
from_way: "Server latency percentiles (p95, p99) from Micrometer and APM tools."
native_way: "Core Web Vitals are latency SLOs measured on the user's device: loading (LCP), responsiveness (INP) and stability (CLS) at p75."
sensei_tip: "Treat them like any SLO: a percentile, a threshold, a dashboard per release and an alert when the budget burns."
---

| Java | In the browser |
| --- | --- |
| p95 request latency | p75 LCP and INP |
| Micrometer timers | `PerformanceObserver` entries |
| A load test | A Lighthouse lab run |
| Production APM | RUM with the web-vitals library |
