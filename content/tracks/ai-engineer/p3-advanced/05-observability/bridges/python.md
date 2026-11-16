---
from: python
status: draft
concept: "See what the model saw"
from_way: "logging, maybe Prometheus metrics and Sentry for exceptions."
native_way: "OpenTelemetry spans around retrieval, model and tool calls with gen_ai attributes, metrics for tokens, cost and latency, and traces linked to user feedback."
sensei_tip: "`with tracer.start_as_current_span('chat') as span: span.set_attribute('gen_ai.usage.input_tokens', n)` is all it takes to start; OpenTelemetry instrumentation packages exist for the main model SDKs."
---

| Python | LLM observability |
| --- | --- |
| `logging` with JSON formatter | structured logs with request ids |
| `opentelemetry-sdk` spans | traces of retrieval, model and tools |
| `prometheus_client` | latency, token and cost metrics |
| Sentry | errors, one signal among several |
| Langfuse SDK | prompt views, costs and evals |
