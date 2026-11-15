---
from: go
status: draft
concept: "Database access with Spring Data JPA"
from_way: "database/sql or sqlc with explicit SQL, and transactions handled by hand."
native_way: "An ORM with managed entities and declarative transactions."
sensei_tip: "Coming from sqlc, JPA's automatic dirty checking can surprise you: changing a loaded entity inside a transaction updates the row without any explicit call."
---

| Go | Spring Boot 4 |
| --- | --- |
| db.QueryRow(...).Scan(&b) | repo.findById(id) |
| tx, _ := db.BeginTx(ctx, nil) | @Transactional |
| sqlc generated queries | derived query methods or @Query |
| golang-migrate | Flyway |
