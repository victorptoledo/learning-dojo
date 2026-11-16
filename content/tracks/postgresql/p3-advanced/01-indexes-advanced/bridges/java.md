---
from: java
status: reviewed
concept: "Choosing an index structure"
from_way: "Putting @Index on every column of a JPA entity, all of them B-trees, and choosing between HashMap, TreeMap and inverted maps in memory."
native_way: "Six index types for six shapes of data and query, plus partial and expression indexes that cover only what you query."
sensei_tip: "JPA's @Index can only create plain B-trees. Write GIN, BRIN, partial and expression indexes in a Flyway or Liquibase migration, with CREATE INDEX CONCURRENTLY outside a transaction."
---

| Java | PostgreSQL |
| --- | --- |
| TreeMap (sorted, range queries) | B-tree |
| HashMap | Hash index (rarely worth it) |
| Map<String, Set<Id>> (an inverted index) | GIN |
| a spatial library's R-tree | GiST |
| min and max kept per chunk of a list | BRIN |
| a filtered stream of only pending jobs | a partial index WHERE status = 'pending' |
