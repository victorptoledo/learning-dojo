---
from: javascript
status: draft
concept: "Background work in Spring"
from_way: "EventEmitter, BullMQ queues and in-memory or Redis caches."
native_way: "Spring events, @Async and @Cacheable."
sensei_tip: "An EventEmitter inside one process is Spring's ApplicationEventPublisher; for work that must survive a restart, use a queue (BullMQ) or an outbox, as in Java."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| emitter.emit('orderPlaced', o) | publisher.publishEvent(new OrderPlaced(o)) |
| emitter.on(...) | @EventListener |
| BullMQ worker | @Async, or a broker consumer |
| node-cache | @Cacheable with Caffeine |
