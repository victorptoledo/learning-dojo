---
from: javascript
status: draft
concept: "Reliability as a number"
from_way: "Logs, APM dashboards and alerts on resource usage."
native_way: "Reliability expressed as SLOs and budgets."
sensei_tip: "Measure at the edge the user sees (the load balancer or the API gateway) as well as inside Node: event loop delays do not show in handler timings."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| prom-client histogram | latency SLI |
| APM dashboard | golden signals |
| alert on memory | alert on burn rate |
| console.error | structured error logs |
