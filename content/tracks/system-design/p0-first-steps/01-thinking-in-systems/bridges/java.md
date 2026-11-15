---
from: java
status: draft
concept: "From one application to a system"
from_way: "Thinking in classes and methods: a call is a nanosecond-cheap stack frame, a failure is an exception with a stack trace."
native_way: "Thinking in processes and networks: a call is a round trip that can be slow, lost or answered twice, and capacity is measured in requests per second."
sensei_tip: "Your JVM already obeys Little's law: a Tomcat pool of 200 threads with 100 ms requests tops out near 2,000 req/s. Size pools from throughput × latency, not by guessing."
---

| Java habit | System design view |
| --- | --- |
| `service.find(id)` | a network round trip, ~0.5 ms in a datacenter |
| `try/catch` around a call | timeouts, retries and partial failure |
| JMH microbenchmark | p50/p99 latency under real load |
| `-Xmx` and heap size | capacity planning: memory per instance × instances |
| one `main` | many instances behind a load balancer |
