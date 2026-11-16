# Database Internals: Storage, Indexes, Logging and Transactions

A database is the place where almost every idea of this track meets: data representation on
disk, caching in a buffer pool, the file system and `fsync`, trees for indexes, a query language
with a parser and an optimiser, and concurrency control for thousands of simultaneous
transactions that must survive crashes. This lesson is conceptual: the SQL and PostgreSQL tracks
cover using a database; here you see how one works inside.

## Pages and the buffer pool

Databases store data in fixed-size **pages** (8 KiB in PostgreSQL, 16 KiB in InnoDB), the unit of
I/O. A table is a **heap file** of pages; each page is a **slotted page**: a header, an array of
slot offsets growing from the front, and the rows (tuples) growing from the back, so rows can move
inside the page without changing their address (page number, slot).

The **buffer pool** caches pages in memory, like the page cache but under the database's control:
it knows which pages are dirty, which must not be evicted yet (pinned), and when a page may be
written (only after its log records, see below). Eviction uses LRU variants or the clock
algorithm, exactly as in virtual memory.

## Indexes: B+trees

A **B+tree** is a balanced search tree with a very high fan-out: each node is a page holding
hundreds of keys, so a tree over a billion rows is only 4 levels deep, and the top levels stay in
the buffer pool. Internal nodes hold keys and child pointers; **leaves** hold the keys with row
pointers (or the rows themselves in a clustered index such as InnoDB's primary key) and are
**linked**, so range scans walk the leaves in order.

- Lookups, inserts and deletes: O(log n) page reads, usually 1–2 actual I/Os.
- Inserting into a full leaf **splits** it in two and pushes a key up; the tree grows at the root
  and stays balanced.
- Updates happen **in place**: random writes, which is fine on SSDs and the reason B+trees power
  PostgreSQL, MySQL, SQL Server, Oracle and SQLite.

## LSM trees

A **log-structured merge tree** turns all writes into sequential ones:

1. Writes go to the log and an in-memory sorted table (the **memtable**).
2. A full memtable is flushed to disk as an immutable sorted file (an **SSTable**).
3. Background **compaction** merges SSTables, dropping overwritten and deleted (tombstoned) keys.
4. Reads check the memtable and then the SSTables from newest to oldest, skipping most of them
   with **Bloom filters**.

LSM trees (RocksDB, LevelDB, Cassandra, ScyllaDB, HBase, and many time-series stores) are built
for write-heavy loads. The trade-off is expressed in three amplifications:

| | B+tree | LSM tree |
| --- | --- | --- |
| Write amplification | page rewrites, random | sequential, but compaction rewrites data several times |
| Read amplification | low: one path down the tree | higher: several levels (Bloom filters help) |
| Space amplification | fragmentation in pages | obsolete versions until compaction |

## The write-ahead log and recovery

Updating pages in place is not atomic and pages are written back lazily. The answer is the same
as journaling in file systems: the **write-ahead log** (WAL).

- Every change is first described in a **log record** (which page, what changed, which
  transaction), appended to a sequential log.
- **WAL rule**: a log record must be on stable storage **before** the page it describes is written,
  and a transaction's **commit record** must be on disk before the commit is acknowledged. Pages
  themselves can be written whenever convenient ("steal, no-force"), which keeps commits fast:
  one sequential `fsync` of the log, shared by many transactions (**group commit**).
- Each record has a **log sequence number** (LSN); each page stores the LSN of the last change
  applied to it, so recovery knows what is already on disk.
- **Checkpoints** periodically record which pages are dirty and which transactions are active, so
  recovery does not have to read the log from the beginning of time.

**Crash recovery** (the ARIES algorithm in outline): **analysis** reads the log from the last
checkpoint to find dirty pages and unfinished transactions; **redo** replays every change after
the checkpoint whose LSN is newer than the page's, restoring the state at the crash; **undo** rolls
back the transactions that never committed. Afterwards the database holds exactly the committed
transactions. The WAL is also what **replication** ships to replicas and what point-in-time
recovery replays.

## Transactions and isolation

A **transaction** groups operations with the **ACID** properties: **atomicity** (all or nothing:
the log and undo), **consistency** (constraints hold), **isolation** (concurrent transactions do
not see each other's partial work) and **durability** (committed means it survives crashes: the
forced log).

Full isolation (**serializability**: the result equals some serial order) is expensive, so SQL
defines weaker levels by the anomalies they allow:

| Level | Dirty read | Non-repeatable read | Phantom | Write skew |
| --- | --- | --- | --- | --- |
| Read uncommitted | possible | possible | possible | possible |
| Read committed (PostgreSQL default) | — | possible | possible | possible |
| Repeatable read / snapshot isolation | — | — | — (in PostgreSQL) | possible |
| Serializable | — | — | — | — |

**Write skew** is the subtle one: two transactions read the same data, each makes a decision based
on it, and each writes something different, so neither sees the other's write (two doctors both
going off call because each saw the other on call).

## Concurrency control: locking and MVCC

- **Two-phase locking** (2PL): a transaction acquires shared locks to read and exclusive locks to
  write, and releases nothing until it has acquired everything (strict 2PL: until commit). This
  guarantees serializability, at the cost of blocking and **deadlocks**, which the database
  detects in its wait-for graph and resolves by aborting a victim.
- **Multi-version concurrency control** (MVCC): writers create a **new version** of a row instead
  of overwriting it; each transaction reads from a **snapshot**, the versions committed when it
  started. Readers never block writers and writers never block readers. PostgreSQL keeps old
  versions in the table (cleaned by `VACUUM`); InnoDB and Oracle keep them in undo logs.
- **Serializable snapshot isolation** (SSI, PostgreSQL's SERIALIZABLE) adds tracking of read/write
  dependencies to MVCC and aborts transactions that could form a non-serializable cycle. The
  application must **retry** them.

## Running a query

A query goes through a pipeline that is a compiler in miniature: **parse** the SQL into a tree,
**rewrite** it (views, rules), **plan** it (the **optimiser** enumerates join orders and access
paths such as a sequential scan, an index scan or a hash join, and estimates the cost of each
from table **statistics**), then **execute** the plan as a tree of operators that pull rows from
their children (the **iterator** or Volcano model), or in batches (vectorised execution). Bad
estimates are the most common reason for a slow query, which is why `ANALYZE` and `EXPLAIN` matter.

## Summary

- Data lives in pages; the buffer pool caches them and controls when they are written.
- B+trees give logarithmic lookups and fast range scans with in-place updates; LSM trees make
  writes sequential at the cost of read and space amplification.
- The write-ahead log, forced at commit and replayed with redo and undo, makes transactions
  atomic and durable.
- Isolation levels trade anomalies for concurrency; write skew survives snapshot isolation.
- Two-phase locking and MVCC are the two ways to isolate transactions; SSI makes MVCC
  serializable.
- A query is parsed, planned with cost estimates and executed as a tree of operators.

## Further reading

- Petrov, *Database Internals: A Deep Dive into How Distributed Data Systems Work*, part I "Storage Engines"
- Kleppmann, *Designing Data-Intensive Applications*, chapters 3 "Storage and Retrieval" and 7 "Transactions"
- Hellerstein, Stonebraker and Hamilton, "Architecture of a Database System" (2007): <https://dsf.berkeley.edu/papers/fntdb07-architecture.pdf>
- Mohan et al., "ARIES: A Transaction Recovery Method Supporting Fine-Granularity Locking and Partial Rollbacks Using Write-Ahead Logging" (1992)
- CMU 15-445/645 Database Systems (Andy Pavlo), lecture notes and videos: <https://15445.courses.cs.cmu.edu/>
- PostgreSQL documentation, "Write-Ahead Logging (WAL)" and "Transaction Isolation": <https://www.postgresql.org/docs/current/wal-intro.html>
