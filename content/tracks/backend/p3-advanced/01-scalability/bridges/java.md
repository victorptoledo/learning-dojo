---
from: java
status: draft
concept: "From one server to many"
from_way: "HttpSession in memory, files written to the server's disk, static caches and counters, and a bigger server when traffic grows."
native_way: "Stateless instances behind a load balancer, state in Redis, object storage and the database, read replicas and sharding when one database is not enough."
sensei_tip: "Estimate the requests per second before redesigning: one well-indexed PostgreSQL handles far more than most people expect."
---

| Single server habit | Horizontally scalable |
| --- | --- |
| `HttpSession` attributes | Redis-backed sessions or tokens |
| `new File("/uploads/" + name)` | object storage (S3-compatible) |
| `static AtomicLong counter` | a counter in Redis or the database |
| `@Scheduled` on every instance | one instance with a lock |
