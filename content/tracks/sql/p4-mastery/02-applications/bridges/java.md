---
from: java
status: draft
concept: "SQL behind application code"
from_way: "JDBC PreparedStatement, JPA and Spring Data repositories, HikariCP and Flyway."
native_way: "Bind parameters, batched queries, keyset pages, pooled connections and versioned migrations."
sensei_tip: "PreparedStatement with ? placeholders is the injection defence; never build JPQL or native queries with string concatenation either. Spring Data's Window and ScrollPosition give you keyset pagination."
---

| Java | SQL |
| --- | --- |
| ps.setString(1, email) | WHERE email = $1 |
| "... WHERE name = '" + name + "'" | SQL injection |
| @EntityGraph / JOIN FETCH | one join instead of N+1 |
| Pageable with page numbers | OFFSET; prefer ScrollPosition.keyset() |
| HikariCP | a connection pool |
| Flyway V2__add_column.sql | a versioned migration |
