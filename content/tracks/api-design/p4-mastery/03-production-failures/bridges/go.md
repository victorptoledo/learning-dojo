---
from: go
status: draft
concept: "API failure modes in production"
from_way: "`http.DefaultClient`, which has no timeout."
native_way: "Clients with `Timeout` and per-request `context` deadlines, backoff with jitter in one place, `sql` statement timeouts, OpenTelemetry and per-client metrics."
sensei_tip: "`http.DefaultClient` never times out; build your own `http.Client{Timeout: ...}` and pass `context` deadlines that shrink as calls go deeper."
---

| Go | Production concern |
| --- | --- |
| `http.Client{Timeout: 5 * time.Second}` | bounded calls |
| `context.WithTimeout` | shrinking deadlines |
| backoff with jitter in one layer | no retry storms |
| OpenTelemetry Go | traces and correlation ids |
| Prometheus labels per client | per-consumer observability |
