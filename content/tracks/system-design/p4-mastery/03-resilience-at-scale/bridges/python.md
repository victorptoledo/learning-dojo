---
from: python
status: draft
concept: "Containing failure"
from_way: "Making one Django or FastAPI service robust with retries and health checks."
native_way: "Containing failures across the system: SLOs, zones and cells, shuffle-sharded tenants, tested regional failover."
sensei_tip: "prometheus_client histograms with a bucket at your latency threshold give the SLI; the error budget burn rate is a PromQL ratio over two windows (for example 1 h and 5 min)."
---

| Python | Resilience concept |
| --- | --- |
| `prometheus_client.Histogram(buckets=[0.3, ...])` | an SLI |
| `random.Random(tenant_id).sample(workers, 2)` | a stable shuffle shard per tenant |
| feature flags (OpenFeature, Unleash client) | degradation switches |
| a tenant → cell mapping table | cell routing |
| `chaostoolkit` | failure experiments |
