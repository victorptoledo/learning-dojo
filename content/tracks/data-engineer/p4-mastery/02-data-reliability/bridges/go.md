---
from: go
status: draft
concept: "Running data in production"
from_way: "Prometheus metrics and alerts on a service's error rate and latency."
native_way: "Metrics and alerts on dataset freshness, volume and quality, with SLOs and runbooks."
sensei_tip: "Export a gauge like data_freshness_seconds{table=\"fct_revenue\"} from your pipeline and alert when it crosses the SLO: the same Prometheus and Alertmanager you already run."
---

| Go | Data reliability |
| --- | --- |
| prometheus gauge | data_freshness_seconds per table |
| Alertmanager route to on-call | data incident alerting |
| canary deploys | comparing new and old outputs |
| pprof for hot paths | query profiles for costly models |
| context.WithTimeout | task timeouts |
