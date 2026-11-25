# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a shop's catalogue, stock and orders. Remove every
   table scan with two indexes, reserve stock in one statement that a concurrent checkout cannot
   fool, save a JSON document only if nobody changed it since it was read, and report on order
   lines kept as JSON arrays.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Indexes and Query Plans | Is this condition sargable? Which column goes first in a composite index? When does an index also give the order? What does `Bitmap Heap Scan` mean? |
| Index Tuning (kata) | Which index serves which query, and which one is not worth its writes? |
| Transactions, Isolation and Locking | Which anomaly does each level prevent? How do you fix a lost update without a big lock? What does a version column buy you? |
| Job Queue (kata) | Why one statement and not a `SELECT` followed by an `UPDATE`? |
| JSON in SQL | `->` or `->>`? Which index serves `@>`? How do you turn an array into rows? When is a column better than a key? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
