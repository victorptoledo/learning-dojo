---
from: java
status: draft
concept: "Spreading load and owning keys"
from_way: "A HashMap picks a bucket with hash mod capacity, and resizing rehashes everything; a server behind a reverse proxy just answers requests."
native_way: "Load balancers spread stateless requests by health and load; stateful tiers place keys on a consistent hash ring so that adding a server moves only about 1/N of them."
sensei_tip: "HashMap's resize moves entries because the bucket count changes: exactly the problem of hash mod N. A TreeMap<Long, String> with ceilingEntry(hash) is a complete consistent hash ring in a dozen lines; you build one in the next kata."
---

| Java | Load balancing concept |
| --- | --- |
| `HashMap` resize | `hash mod N` remapping when servers change |
| `TreeMap.ceilingEntry(h)` | the next node clockwise on a ring |
| Spring Boot Actuator `/actuator/health/readiness` | the instance's own health check |
| `server.shutdown=graceful` | connection draining on deploy |
| Spring Cloud LoadBalancer | client-side balancing |
