---
from: go
status: draft
concept: "Reliability is a target with a budget"
from_way: "You count errors in your handler and log when they seem high."
native_way: "The handler records a counter with the status code and a histogram of durations; recording rules and burn-rate alerts turn them into an SLO."
sensei_tip: "promhttp.InstrumentHandlerDuration with buckets that include your threshold gives a latency SLI in one line."
---

| Go world | SRE |
| --- | --- |
| an error counter | the SLI's bad events |
| `prometheus.HistogramOpts{Buckets: ...}` | the latency threshold |
| a log line "too many errors" | a burn-rate alert |
| a TODO in the backlog | error budget policy work |
