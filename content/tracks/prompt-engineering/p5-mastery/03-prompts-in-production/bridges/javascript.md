---
from: javascript
status: draft
concept: "Observability for a flaky upstream"
from_way: "fetch calls with AbortController timeouts, retries, and dashboards of response times and error rates."
native_way: "Treat the model as a flaky upstream whose failures are often silent: validate outputs, alert on parse failures and stop reasons, track tokens and cost, and keep a fallback ready."
sensei_tip: "Track time to first token separately from total latency when you stream: users feel the first, your bill and your timeouts feel the second."
---

| JavaScript | Production concept |
| --- | --- |
| `AbortSignal.timeout(ms)` | timeouts on model calls |
| retry with backoff | bounded retries |
| OpenTelemetry spans | a trace per model call |
| streaming responses | perceived latency |
| a feature flag kill switch | degraded mode |
