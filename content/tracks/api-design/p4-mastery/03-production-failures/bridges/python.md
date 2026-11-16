---
from: python
status: draft
concept: "API failure modes in production"
from_way: "`requests` without timeouts (the default is to wait forever)."
native_way: "Timeouts on every `httpx` call, tenacity with jitter in exactly one layer, statement timeouts in the database, OpenTelemetry traces and per-client metrics."
sensei_tip: "`requests.get(url)` has no timeout by default; `httpx` defaults to five seconds. Always pass one explicitly."
---

| Python | Production concern |
| --- | --- |
| `httpx.Timeout(...)` | bounded calls |
| `tenacity` in one layer | no retry storms |
| `statement_timeout` | bounded queries |
| OpenTelemetry | traces and correlation ids |
| Prometheus labels per client | per-consumer observability |
