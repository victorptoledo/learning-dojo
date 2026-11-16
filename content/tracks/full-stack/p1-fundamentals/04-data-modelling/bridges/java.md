---
from: java
status: reviewed
concept: "From JPA entities to page queries"
from_way: "Entities with lazy collections and ddl-auto=update, and a list page that triggers one query per row."
native_way: "Flyway or Liquibase migrations own the schema, constraints live in it, and list pages use fetch joins, projections or native JSON queries with keyset paging."
sensei_tip: "Set spring.jpa.properties.hibernate.default_batch_fetch_size and log the SQL: N+1 shows up as a wall of identical selects."
---

| JPA habit | Full stack practice |
| --- | --- |
| `ddl-auto=update` | versioned Flyway migrations |
| `@OneToMany` lazy loop | `join fetch`, a DTO projection or batch fetching |
| `PageRequest.of(page, 20)` (offset) | keyset: `findByIdLessThan(..., Limit)` |
| validation only in `@Valid` | plus `CHECK` and `NOT NULL` in the schema |
