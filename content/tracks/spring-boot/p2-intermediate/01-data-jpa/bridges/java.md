---
from: java
status: draft
concept: "From JDBC and DAOs to Spring Data"
from_way: "JDBC with Connection, PreparedStatement and ResultSet mapping by hand, DAO classes with one method per query, and commit/rollback in try/finally."
native_way: "JPA entities, repository interfaces implemented by Spring Data from their method names, and @Transactional service methods."
sensei_tip: "Turn on spring.jpa.show-sql (or logging.level.org.hibernate.SQL=debug) while learning: seeing the SQL is the fastest way to understand what JPA does."
---

| JDBC / DAO | Spring Data JPA |
| --- | --- |
| `rs.getString("title")` mapping by hand | `@Entity` fields mapped automatically |
| `BookDao.findByIsbn` with SQL | `Optional<Book> findByIsbn(String isbn);` |
| `conn.setAutoCommit(false); ... conn.commit();` | `@Transactional` |
| `javax.persistence` | `jakarta.persistence` |
| Update statements for every change | Dirty checking at commit |
