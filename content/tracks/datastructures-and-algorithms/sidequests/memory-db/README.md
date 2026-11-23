# Sidequest: In-memory database

Under every database there are a few data structures doing all the work: an ordered index
(a B-tree in PostgreSQL, a skip list in Redis and in the memtable of LevelDB and RocksDB),
bitmaps for columns with few distinct values (as in Apache Druid, Pinot and many column stores),
and a planner that guesses which index will return the fewest rows before it reads any of them.
In this sidequest you write them, in Java, and the tests hold you to their costs.

Build, in package `dojo.db`:

- **Stage 1:** `OrderedIndex<K, V>`, a sorted map written by hand (a skip list is suggested; a
  balanced tree or a B-tree is fine). The tests count its comparisons.
- **Stage 2:** lazy range scans, `rank` and `select` in O(log n).
- **Stage 3:** `Table`, whose rows live in a `RowStore` that the tests count reads from, with
  secondary indexes that keep the rows read down to the rows that can match.
- **Stage 4:** bitmap indexes over `long` words, combined with AND and OR and counted with
  `Long.bitCount`, so counts read no row at all.
- **Stage 5:** a planner that picks the most selective index from counts it gets in O(log n),
  and top-k queries that stop early.

The costs are measured, not reviewed: a counting `Comparator`, a counting `RowStore`, and time
limits on hundreds of thousands of rows inside a 256 MB heap. Use the JDK only, but **not** its
sorted collections (`TreeMap`, `TreeSet`, `ConcurrentSkipListMap`, `ConcurrentSkipListSet`,
`PriorityQueue` inside the index) nor `java.util.BitSet`: the tests look inside your objects for
them. JUnit is there for your own tests.
