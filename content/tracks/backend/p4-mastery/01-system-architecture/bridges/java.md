---
from: java
status: draft
concept: "Architecture choices"
from_way: "One big shared model (one Product class for everyone), modules that read each other's tables, or microservices split by entity with chains of synchronous calls."
native_way: "Bounded contexts that own their data, a modular monolith first, events between contexts, sagas with compensations for cross-service workflows, and ADRs for decisions."
sensei_tip: "If two services must always be deployed together, they are one service with a network call in the middle."
---

| Smell | Better |
| --- | --- |
| `Product` with 80 fields used by every module | one model per bounded context |
| `select * from orders` in the shipping module | `OrderPlaced` events or the orders API |
| XA/2PC across services | a saga with compensations |
| Decisions only in someone's head | ADRs in the repository |
