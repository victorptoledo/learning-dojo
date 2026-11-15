---
from: go
status: draft
concept: "From one server to many"
from_way: "A fast single binary that handles a lot on one machine."
native_way: "Many replicas, shared state outside the process, and partitioning when needed."
sensei_tip: "Go's efficiency delays scaling, but the same rules apply: keep instances stateless and measure before sharding."
---

| Go | In practice |
| --- | --- |
| one binary on a big VM | replicas behind a load balancer |
| sync.Map state | shared store |
| pgxpool on one database | replicas and connection limits |
| hash(key) % n | consistent hashing |
