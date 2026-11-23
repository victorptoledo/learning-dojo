Now a table on top of the index. The starter adds:

- `RowStore`, where the rows live, like the pages of a database on disk: `append`, `read` and
  `delete`. Reading is the expensive part, and the contract tests give the table a store that
  **counts every read**. `MemoryRowStore` is a plain one for your own tests.
- `Predicate`, a sealed interface with `Eq(column, value)`, `Range(column, from, to)` for
  `from <= value < to`, and `In(column, values)`.
- `Table`, with the methods to fill in.

| Method | Does |
| --- | --- |
| `Table(List<String> columns, RowStore store)` | A table of `long` columns. No columns, repeated names or a `null` store is an `IllegalArgumentException`. |
| `int insert(long... values)` | Appends the row to the store and returns its id (the store's). The wrong number of values is an `IllegalArgumentException`. |
| `long[] get(int row)` | The row's values. A deleted or unknown id is an `IllegalArgumentException`. |
| `boolean delete(int row)` | Deletes the row from the store and from every index; `false` when there is no such row. |
| `int size()` | Rows alive. |
| `void createIndex(String column)` | An ordered index on the column, built from the rows already there and kept up to date. An unknown or already indexed column is an `IllegalArgumentException`. |
| `List<Integer> query(List<Predicate> where)` | The ids of the rows that match **every** predicate, ascending. An empty list matches every row. An unknown column is an `IllegalArgumentException`. |

**The rows stay in the store.** The table may keep the ids of its rows and its indexes (values
and row ids), never copies of the rows: anything else would hide reads.

**Reads.** Without an index, a query reads every row (a full scan). With an index on the column of
a predicate, it must read **only rows that predicate matches**: take them from the index (the
entries `(value, row)` from `(from, any row)` to `(to, any row)`, a range of stage 2) and read
those to check the other predicates. The tests count: an `Eq` on an indexed column that matches 40
rows of 100,000 may read at most 40 rows.

**Keys with duplicates.** Many rows share a value, but the index needs distinct keys: index the
pair `(value, row id)`, ordered by value and then by row. An `Eq(c, v)` is then the range from
`(v, Integer.MIN_VALUE)` to `(v + 1, Integer.MIN_VALUE)`.
