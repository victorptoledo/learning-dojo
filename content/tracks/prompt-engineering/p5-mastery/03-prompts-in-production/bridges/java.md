---
from: java
status: draft
concept: "Operating a dependency you do not control"
from_way: "Resilience4j circuit breakers, timeouts and fallbacks around an external HTTP service, with Micrometer metrics and alerts."
native_way: "The model is an external dependency: guardrails validate its input and output, fallbacks and circuit breakers handle failures, and metrics on stop reasons, parse failures, tokens and latency reveal silent problems."
sensei_tip: "Add a Micrometer counter tagged with prompt id, prompt version and model for parse failures and for each stop reason; most prompt incidents show up there hours before a user complains."
---

| Java | Production concept |
| --- | --- |
| Resilience4j `CircuitBreaker` | stop calling a failing provider |
| `@TimeLimiter` | timeouts on model calls |
| a fallback method | a fallback model or degraded mode |
| Micrometer counters and timers | parse failures, stop reasons, latency |
| log masking of PII | privacy in prompts and logs |
