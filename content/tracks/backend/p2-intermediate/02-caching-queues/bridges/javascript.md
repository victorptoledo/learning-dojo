---
from: javascript
status: draft
concept: "Non-blocking operations"
from_way: "node-cache or Redis with ioredis, and BullMQ queues."
native_way: "Caching patterns and queues with clear staleness and failure rules."
sensei_tip: "Coalesce concurrent misses for the same key (keep one pending promise per key): that is single flight, the simplest stampede protection."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| node-cache | in-process cache |
| redis.get then set | cache-aside |
| one Promise per key | single flight |
| BullMQ | a work queue |
