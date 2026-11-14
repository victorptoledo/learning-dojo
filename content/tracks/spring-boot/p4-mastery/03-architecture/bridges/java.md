---
from: java
status: draft
concept: "Structuring a Spring application"
from_way: "controllers/, services/ and repositories/ packages with everything public, or one microservice per entity from day one."
native_way: "Feature packages with package-private internals, a modular monolith verified by Spring Modulith, events between modules, ports and adapters where the domain is rich."
sensei_tip: "Add the Modulith verify() test on day one: boundaries are cheap to keep and expensive to restore."
---

| Before | Now |
| --- | --- |
| `com.shop.services.*` all public | `com.shop.orders` with a small public API |
| `orderService.notifyShipping()` | `publishEvent(new OrderPlaced(...))` |
| Hand-written outbox tables | Modulith event publication registry |
| Microservices first | Modular monolith first, split with a reason |
