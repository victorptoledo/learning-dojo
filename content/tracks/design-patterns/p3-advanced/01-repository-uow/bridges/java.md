---
from: java
status: draft
concept: "Repository and unit of work"
from_way: "DAOs with SQL strings called from controllers, `JpaRepository` injected everywhere, and a `save` after every setter."
native_way: "Small domain repository interfaces per aggregate, implemented with Spring Data or JDBC in an adapter, and `@Transactional` around each use case so the persistence context is the unit of work."
sensei_tip: "Turn on `spring.jpa.show-sql` (or a datasource proxy) once in every new project and read what your use cases really send: dirty checking, N+1 and flush order stop being magic."
---

| Java | Pattern |
| --- | --- |
| `interface Accounts { Optional<Account> find(...); }` | repository |
| Spring Data `JpaRepository` | generated repository (broad) |
| `EntityManager` / Hibernate `Session` | unit of work, identity map |
| `@Transactional` on the service method | unit of work boundary |
| `JdbcTemplate` DAO | table data gateway |
| Panache entities (Quarkus) | active record |
