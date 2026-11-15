---
from: go
status: draft
concept: "Reliability as a number"
from_way: "Prometheus metrics and Grafana dashboards, often already in place."
native_way: "SLOs, error budgets and alerting on symptoms."
sensei_tip: "Go services usually expose Prometheus histograms; the step is to write recording rules for good/total ratios and multi-window burn-rate alerts."
---

| Go | In practice |
| --- | --- |
| prometheus.NewHistogram | latency SLI |
| Grafana panels | SLO dashboards |
| alert on goroutines | alert on budget burn |
| log/slog | structured logs with trace ids |
