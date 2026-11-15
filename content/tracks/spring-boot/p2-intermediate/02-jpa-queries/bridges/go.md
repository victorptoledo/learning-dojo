---
from: go
status: draft
concept: "Joins, queries and pages"
from_way: "Hand-written SQL with joins and LIMIT/OFFSET."
native_way: "Generated or JPQL queries with join fetch and paging."
sensei_tip: "Writing the join yourself in Go avoids N+1 by construction; with JPA, lazy loading makes N+1 easy, so fetch joins and projections become your habit."
---

| Go | Spring Boot 4 |
| --- | --- |
| SELECT ... JOIN authors | join fetch in JPQL |
| scan into a small struct | record projection |
| LIMIT/OFFSET | Pageable |
| sqlx.Select | repository query method |
