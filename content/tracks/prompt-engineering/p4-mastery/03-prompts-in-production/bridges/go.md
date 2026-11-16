---
from: go
status: draft
concept: "Context deadlines and metrics"
from_way: "context.WithTimeout on outbound calls, Prometheus counters and histograms, and graceful degradation."
native_way: "Every model call has a deadline, a bounded retry and a fallback; Prometheus metrics labelled by prompt version and model track stop reasons, parse failures, tokens and latency."
sensei_tip: "Keep metric labels small and stable (prompt id, version, model, stop reason); never put user input or request ids in labels, or your metrics backend becomes the incident."
---

| Go | Production concept |
| --- | --- |
| `context.WithTimeout` | deadlines on model calls |
| a retry loop with a maximum | bounded retries |
| Prometheus counters with labels | parse failures and stop reasons per version |
| a histogram | latency and token distributions |
| `singleflight` and a cache | avoiding duplicate expensive calls |
