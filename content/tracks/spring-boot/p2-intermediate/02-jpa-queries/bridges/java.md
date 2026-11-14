---
from: java
status: draft
concept: "Joins, queries and pages"
from_way: "Hand-written SQL joins mapped row by row, LIMIT and OFFSET built into strings, and object graphs loaded eagerly to be safe."
native_way: "Associations mapped LAZY, fetch joins or entity graphs per use case, JPQL with named parameters, and Pageable/Page."
sensei_tip: "Count the SQL statements one request makes; if it grows with the number of rows, you have an N+1."
---

| SQL habit | JPA |
| --- | --- |
| `select ... from books b join authors a on ...` | `select b from Book b join fetch b.author` |
| `"... where name = '" + name + "'"` | `where b.author.name = :name` + `@Param` |
| `limit 20 offset 40` | `PageRequest.of(2, 20)` |
| Mapping a join row into a DTO | `select new dojo.BookView(b.title, a.name) ...` |
