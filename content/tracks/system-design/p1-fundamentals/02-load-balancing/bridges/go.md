---
from: go
status: draft
concept: "Spreading load and owning keys"
from_way: "httputil.ReverseProxy forwarding to one backend; maps hashing keys into buckets."
native_way: "Balancers pick backends by health and load; keyed tiers use a consistent hash ring with virtual nodes."
sensei_tip: "groupcache (by the author of memcached) uses a consistent hash ring to decide which peer owns a key; its consistenthash package is about 80 lines and worth reading after the kata."
---

| Go | Load balancing concept |
| --- | --- |
| `httputil.ReverseProxy` with a `Director` | an L7 balancer in miniature |
| `sort.Search(len(ring), ...)` | next node clockwise |
| `grpc` round_robin balancer | client-side per-request balancing |
| `srv.Shutdown(ctx)` | graceful draining |
| `hash/fnv`, `crypto/md5` | stable hashes for placement |
