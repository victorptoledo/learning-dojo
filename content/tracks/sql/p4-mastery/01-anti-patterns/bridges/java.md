---
from: java
status: draft
concept: "Queries that look right and are not"
from_way: "Code review habits for Java: null checks, equals versus ==, integer overflow, N+1 in Hibernate."
native_way: "The same reflexes for SQL: NULL logic, fan-out, sargable filters, half-open ranges."
sensei_tip: "Most Java SQL bugs are the ORM's defaults: lazy collections looped in a view, SELECT * entities, and findAll() followed by a stream filter. Read the SQL log."
---

| Java | SQL |
| --- | --- |
| findAll().stream().filter(...) | a WHERE clause |
| count() > 0 to check existence | EXISTS |
| double for money | numeric (BigDecimal) |
| Collectors.toSet() to remove duplicate join rows | DISTINCT as a bandage: fix the join |
| 1 / 3 == 0 with ints | integer division in SQL too |
