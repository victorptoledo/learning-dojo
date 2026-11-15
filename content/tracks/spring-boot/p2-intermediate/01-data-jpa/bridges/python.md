---
from: python
status: draft
concept: "Database access with Spring Data JPA"
from_way: "The Django ORM or SQLAlchemy with sessions and Alembic migrations."
native_way: "JPA entities with Hibernate, Spring Data repositories, and @Transactional services."
sensei_tip: "A Hibernate persistence context is a SQLAlchemy Session: changes to managed entities are flushed at commit without calling save. @Transactional is session.begin() around the method."
---

| Python | Spring Boot 4 |
| --- | --- |
| class Book(Model) | @Entity class Book |
| Book.objects.get(pk=1) | bookRepository.findById(1L) |
| session.commit() | end of a @Transactional method |
| Alembic / Django migrations | Flyway or Liquibase |
| lazy relationships | FetchType.LAZY |
