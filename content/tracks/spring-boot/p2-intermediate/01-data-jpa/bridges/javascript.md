---
from: javascript
status: draft
concept: "Database access with Spring Data JPA"
from_way: "Prisma, TypeORM or Sequelize, with repositories or a client per model."
native_way: "JPA entities and Spring Data repositories generated from interfaces."
sensei_tip: "TypeORM's Repository<Book> is Spring Data's JpaRepository<Book, Long>, and Spring generates the implementation, including query methods from their names."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| @Entity() class Book | @Entity class Book |
| repo.findOneBy({ id }) | repo.findById(id) |
| prisma.$transaction | @Transactional |
| prisma migrate | Flyway |
