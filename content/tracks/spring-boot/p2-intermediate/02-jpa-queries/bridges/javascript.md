---
from: javascript
status: draft
concept: "Joins, queries and pages"
from_way: "TypeORM relations and QueryBuilder, or Prisma include and select."
native_way: "JPQL, fetch joins, projections and Pageable."
sensei_tip: "Prisma's include is a fetch join and select is a projection. Turn on spring.jpa.show-sql (or a SQL logger) the way you would inspect Prisma's query log."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| include: { author: true } | join fetch |
| select: { title: true } | record projection |
| skip / take | PageRequest.of(page, size) |
| QueryBuilder | @Query with JPQL or Criteria |
