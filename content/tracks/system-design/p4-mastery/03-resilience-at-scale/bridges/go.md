---
from: go
status: draft
concept: "Containing failure"
from_way: "Robust handlers with context deadlines and recovers from panics in one service."
native_way: "Containment across the fleet: SLOs, cells, shuffle sharding, static stability across zones and tested regional failover."
sensei_tip: "A deferred recover() keeps one panicking request from killing the server, but a poison request retried everywhere still kills every worker it reaches. Shuffle sharding limits how many workers that is."
---

| Go | Resilience concept |
| --- | --- |
| `prometheus/client_golang` histograms | SLIs |
| `defer func() { recover() }()` per request | isolating one request |
| a stable hash of tenant id → worker subset | shuffle sharding |
| `errgroup` with partial results | graceful degradation |
| a cell router in front of identical stacks | cells |
