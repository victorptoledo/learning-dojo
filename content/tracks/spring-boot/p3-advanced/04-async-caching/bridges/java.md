---
from: java
status: draft
concept: "Background work in Spring"
from_way: "Calling every interested service directly, new Thread(...) or a hand-made executor for slow work, Timer and Quartz for schedules, and a static HashMap as a cache."
native_way: "Records published as application events, @TransactionalEventListener, @Async on virtual threads, @Scheduled with cron, and @Cacheable backed by Caffeine or Redis."
sensei_tip: "If a listener must never lose an event, an in-memory event is not enough: write it to the database in the same transaction (outbox)."
---

| Before | Spring Boot 4 |
| --- | --- |
| `mailService.send(...)` inside `OrderService` | `events.publishEvent(new OrderPlaced(...))` |
| `new Thread(() -> build()).start()` | `@Async` (virtual threads with `spring.threads.virtual.enabled`) |
| `java.util.Timer` / Quartz for a nightly job | `@Scheduled(cron = "0 0 3 * * *")` |
| `static Map<String, Price> CACHE` | `@Cacheable("prices")` + Caffeine |
