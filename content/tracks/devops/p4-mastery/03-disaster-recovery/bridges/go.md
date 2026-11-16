---
from: go
status: draft
concept: "Recovery is designed, tested and timed"
from_way: "Your service is stateless, so it seems there is nothing to recover."
native_way: "The data it depends on, its configuration, its images and its DNS all need a recovery plan, and chaos experiments check its timeouts and retries."
sensei_tip: "Inject latency with a test round-tripper (http.RoundTripper) in staging to see whether your context timeouts and retries behave."
---

| Go world | Disaster recovery |
| --- | --- |
| a stateless binary | easy to rebuild from IaC |
| `context.WithTimeout` | bounding failures |
| a fake RoundTripper | fault injection in tests |
| `kubectl delete pod` | a first chaos experiment |
