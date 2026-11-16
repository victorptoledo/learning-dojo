---
from: java
status: draft
concept: "Running data in production"
from_way: "Micrometer metrics, health checks and on-call for a service's latency and errors."
native_way: "SLOs for freshness, completeness and correctness, data observability, and incident practice for datasets."
sensei_tip: "Your SRE habits transfer: an SLO, an error budget and a runbook; add checks on the data itself, because a batch job can return exit code 0 with half the rows."
---

| Java | Data reliability |
| --- | --- |
| Micrometer timers | pipeline duration metrics |
| /actuator/health | freshness and quality status of a dataset |
| latency SLO | freshness SLO |
| blue-green deploy | build new model beside old, compare, switch |
| feature flags | switching consumers to a new table version |
