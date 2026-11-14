---
from: java
status: draft
concept: "Persistence model"
from_way: "Mapping everything to ORM entities, read-modify-save for every change, @Transactional everywhere without knowing the isolation level, and a NoSQL database added because it is fashionable."
native_way: "Atomic updates, version columns or row locks chosen per use case, constraints as the backstop, retries for serialization failures, and NoSQL stores only for measured needs."
sensei_tip: "Before adding a NoSQL database, ask whether Postgres with JSONB columns and GIN indexes already solves it."
---

| Java habit | Database-aware |
| --- | --- |
| `p.setStock(p.getStock() - 1); save(p)` under load | `update ... set stock = stock - 1 where stock > 0` |
| `if (!repo.existsByEmail(e)) repo.save(...)` | a `unique` constraint and a 409 on violation |
| `synchronized` around a DB update | `@Version` or `select ... for update` |
| "We need MongoDB for flexible data" | a `jsonb` column first |
