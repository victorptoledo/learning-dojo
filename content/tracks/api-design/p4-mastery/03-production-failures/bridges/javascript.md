---
from: javascript
status: draft
concept: "API failure modes in production"
from_way: "`fetch` without `AbortSignal`, retries in the SDK and in the gateway."
native_way: "`AbortSignal.timeout` on each call, a circuit breaker (opossum), retries with jitter in one place, and OpenTelemetry metrics per client and route."
sensei_tip: "One slow dependency can block the event loop's sockets; a circuit breaker (`opossum`) fails fast instead of piling up pending promises."
---

| JavaScript / TypeScript | Production concern |
| --- | --- |
| `AbortSignal.timeout(ms)` | bounded calls |
| `opossum` | circuit breaker |
| retries with jitter in one layer | no retry storms |
| OpenTelemetry | traces and correlation ids |
| metrics labelled by client | per-consumer observability |
