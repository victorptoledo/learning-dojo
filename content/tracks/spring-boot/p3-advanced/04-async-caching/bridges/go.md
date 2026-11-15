---
from: go
status: draft
concept: "Background work in Spring"
from_way: "Goroutines for background work, channels, and simple in-memory caches."
native_way: "Events, @Async executors and declarative caching."
sensei_tip: "go sendEmail(o) after commit is @Async plus @TransactionalEventListener; the difference is that Spring makes the after-commit timing explicit."
---

| Go | Spring Boot 4 |
| --- | --- |
| go sendEmail(o) | @Async listener |
| channel of events | ApplicationEventPublisher |
| sync.Map cache | @Cacheable |
| time.Ticker job | @Scheduled |
