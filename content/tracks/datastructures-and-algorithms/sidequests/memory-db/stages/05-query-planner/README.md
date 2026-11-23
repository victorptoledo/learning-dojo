A database decides how to run a query before it runs it. For `WHERE score BETWEEN 10 AND 20 AND
city = 3` with an index on each column, it asks each index how many rows it would return and
starts from the smallest: the **most selective** index drives the query. The question must be
cheap, so it never reads a row and never walks the rows it counts. The starter adds the record
`Plan(String drivingColumn, long estimatedRows)`; add to `Table`:

| Method | Does |
| --- | --- |
| `Plan explain(List<Predicate> where)` | Among the predicates whose column has an index, the one that matches the **fewest rows**, with that number (exact); ties go to the first in the list. With no indexed predicate, `Plan(null, size())`: a full scan. Bad predicates as in `query`. |
| `List<Integer> top(List<Predicate> where, String orderBy, int limit)` | The rows of `query(where)` with the smallest values of `orderBy` (ties by row id), in that order, at most `limit` of them. An unknown column or a negative `limit` is an `IllegalArgumentException`. |

**Counting in O(log n).** An ordered index counts a range without walking it: the rows of
`[from, to)` are `rank((to, MIN)) - rank((from, MIN))`, two searches of stage 2, and an `In` adds
up one count per value. A bitmap index counts with the cardinality of its bitmaps: keep it in each
bitmap, updated when a bit really changes, so nothing is counted twice. When a column has both
kinds of index, either gives the same number. The tests call `explain` 100,000 times on 500,000
rows with ranges that match half the table: walking the range, or counting bits each time, is too
slow.

**Top-k.** When `orderBy` has an ordered index and every predicate is indexed, walk that index in
order from the start, keep the rows that the predicates allow (a bitmap lookup each), and stop at
`limit`: no row is read, and a query whose first matches come early costs almost nothing.
Otherwise read the rows that the indexed predicates allow and keep the best `limit` in a
**bounded max-heap** of `(value, row)`: push each row, and when the heap holds more than `limit`,
pop the worst. That is O(n log k) time and O(k) memory, not a sort of every match.

```java
// a planner on people(id, city, age, score), bitmap on city, ordered on age and score
table.explain(List.of(new Predicate.Eq("city", 3), new Predicate.Range("score", 0, 500)));
// Plan[drivingColumn=score, ...]: about 0.5% of the rows, against 10% for city
table.top(List.of(new Predicate.Eq("city", 3)), "age", 5);   // the five youngest in city 3
```
