---
from: csharp
status: draft
concept: "Resilience pipelines"
from_way: "Polly resilience pipelines with retry, timeout and circuit breaker around HttpClient, and Application Insights telemetry."
native_way: "Model calls inside a resilience pipeline, with guardrails on input and output, a fallback, and telemetry per call that includes prompt version, model, tokens and stop reason."
sensei_tip: "Add the prompt version and model as custom dimensions on your telemetry; a dashboard split by those two dimensions makes 'the provider changed something' obvious."
---

| C# | Production concept |
| --- | --- |
| Polly retry and timeout | bounded retries and timeouts |
| Polly circuit breaker | stop calling a failing provider |
| Polly fallback | a fallback model or degraded mode |
| Application Insights custom dimensions | prompt version and model per call |
| `IMemoryCache` | caching repeated answers |
