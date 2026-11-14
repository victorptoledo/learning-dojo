---
from: java
status: draft
concept: "Thinking in tables"
from_way: "Designing Java classes first and letting the ORM generate whatever schema follows, with validation only in Java and no thought about indexes."
native_way: "A schema designed with keys and constraints, SQL you can read and write, indexes chosen for real queries and verified with explain analyze."
sensei_tip: "Turn on the SQL log while developing, copy the slowest query into explain analyze, and look for Seq Scan."
---

| Java habit | Database view |
| --- | --- |
| `@NotNull` only on the entity | `not null` in the table too |
| `List<Order>` everywhere | a foreign key and an index on `customer_id` |
| `if (repo.existsByEmail(e)) throw ...` | a `unique` constraint (the check alone races) |
| `findAll().stream().filter(...)` | a `where` clause that uses an index |
