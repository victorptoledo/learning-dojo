---
from: csharp
status: draft
concept: "Background work in Spring"
from_way: "MediatR notifications, hosted services and IMemoryCache or IDistributedCache."
native_way: "Spring events, @Async and the cache abstraction."
sensei_tip: "MediatR's INotification handlers are @EventListener methods; IMemoryCache with GetOrCreate is @Cacheable with Caffeine."
---

| C# | Spring Boot 4 |
| --- | --- |
| mediator.Publish(new OrderPlaced()) | publishEvent(new OrderPlaced()) |
| INotificationHandler<T> | @EventListener |
| BackgroundService | @Async / @Scheduled |
| IMemoryCache.GetOrCreate | @Cacheable |
