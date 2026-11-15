# Storage Engines, Indexes and Data Models

Choosing a database is mostly choosing two things: a **data model** (how you describe and query
data) and a **storage engine** (how bytes are laid out on disk). The model decides what is easy
to ask; the engine decides what is cheap to write, read and keep. Knowing both lets you predict
how a database behaves before you benchmark it.

## Data models

| Model | Shape | Strong at | Weak at | Examples |
| --- | --- | --- | --- | --- |
| Relational | tables, rows, joins, SQL | ad hoc queries, constraints, transactions | horizontal scaling of writes (needs sharding) | PostgreSQL, MySQL |
| Key-value | `key → opaque value` | very fast lookups by key, simple to partition | anything but "get by key" | Redis, DynamoDB (core), etcd |
| Document | JSON-like trees, one document per entity | data read together, flexible schema | joins across documents, many-to-many | MongoDB, Couchbase |
| Wide-column | rows with many sparse columns, grouped by a partition key | huge write volume, time series by key | ad hoc queries, secondary access paths | Cassandra, HBase, Bigtable |
| Graph | nodes and edges | traversals (friends of friends, fraud rings) | bulk scans and aggregations | Neo4j, Neptune |
| Search index | inverted index of terms | full-text search, facets | being the system of record | Elasticsearch, OpenSearch |
| Object storage | blobs addressed by key, with metadata | cheap, durable, huge files | low-latency small updates | S3, GCS, Azure Blob |

Rules of thumb: model **access patterns first**. If every read is "get the order with its
lines", a document fits; if the business asks new questions every week, keep a relational
model and copy data into specialised stores (search, analytics) as needed. Store big files
(photos, videos, backups) in object storage and keep only their keys in the database.

## Two families of storage engines

### B-trees: update in place

A **B-tree** keeps keys sorted in fixed-size pages (4–16 KB) arranged as a shallow tree: a
billion keys fit in 4 levels. A read walks from the root to a leaf, a few page reads (usually
cached). A write finds the page and changes it **in place**, after first appending the change
to a **write-ahead log** (WAL) so a crash can be repaired.

### LSM trees: append, then merge

A **log-structured merge tree** never updates in place:

```text
write → WAL (append) + memtable (sorted, in memory)
        memtable full → flushed as an immutable sorted file (SSTable) on disk
        background compaction merges SSTables, drops overwritten and deleted keys
read  → memtable → newest SSTable → older SSTables ... (Bloom filters skip files without the key)
```

Deletes write a **tombstone**, removed later by compaction.

| | B-tree | LSM tree |
| --- | --- | --- |
| Writes | random page writes, each a read-modify-write | sequential appends; very high throughput |
| Reads | predictable: one path through the tree | may check several files; Bloom filters help |
| Space | pages partly empty, fragmentation | old versions until compaction |
| Background work | little (vacuum in PostgreSQL) | compaction, which competes with traffic |
| Amplification | write amplification from whole-page writes | write amplification from repeated merges |
| Used by | PostgreSQL, MySQL InnoDB, SQL Server | RocksDB, Cassandra, ScyllaDB, LevelDB, HBase |

Neither wins in general; heavy ingestion (events, metrics, messages) leans LSM, mixed OLTP
with many point reads and range scans leans B-tree.

## Indexes

An index is a second, smaller structure that answers "where is the row with this value?".

| Index | Answers | Note |
| --- | --- | --- |
| Primary / clustered | rows stored in key order | range scans by key are sequential |
| Secondary (B-tree) | equality, ranges, sort order, prefixes | every index slows every write |
| Composite `(a, b)` | `a = ?`, `a = ? AND b > ?` | not `b = ?` alone: column order matters |
| Covering | the query reads only the index | `INCLUDE` columns in PostgreSQL |
| Hash | equality only | fast, no ranges |
| Inverted | which documents contain a term | search engines, GIN in PostgreSQL |
| Geospatial | points within a region | R-trees, geohashes, H3 cells |

In a partitioned database a secondary index is either **local** (each partition indexes its own
rows: writes are cheap, a query asks every partition) or **global** (the index is partitioned
by the indexed value: reads hit one partition, writes update a remote index, usually
asynchronously).

## OLTP versus OLAP

| | OLTP (transactions) | OLAP (analytics) |
| --- | --- | --- |
| Query | few rows by key, many small writes | scans of millions of rows, aggregates |
| Layout | row-oriented | column-oriented, compressed |
| Users | the application | analysts, dashboards, ML pipelines |
| Examples | PostgreSQL, MySQL, DynamoDB | BigQuery, Snowflake, ClickHouse, Redshift |

Don't run heavy analytics on the production database: stream changes (change data capture)
into a warehouse or a lake and query there.

## Durability in one table

| Mechanism | Protects against |
| --- | --- |
| WAL + `fsync` before acknowledging | a process or machine crash |
| Replication to other machines / zones | a disk or machine loss, a zone outage |
| Backups and point-in-time recovery | bugs and mistakes (`DELETE` without `WHERE`), ransomware |
| Object storage (11 nines of durability claimed by S3) | losing files at scale |

Replication is not a backup: a bad `DELETE` replicates in milliseconds.

## Probabilistic structures: Bloom filters

An LSM tree must not open every SSTable to learn that a key is absent. A **Bloom filter** is a
bit array plus `k` hash functions: adding a key sets `k` bits; asking about a key checks them.

- If any bit is 0, the key is **definitely absent**.
- If all are 1, it is **probably present** (other keys may have set those bits).
- No false negatives, a tunable false-positive rate, and about **10 bits per key for 1 %**.

| Elements `n`, false-positive rate `p` | Bits `m = −n·ln p / (ln 2)²` | Hashes `k = (m/n)·ln 2` |
| --- | --- | --- |
| 1,000 at 1 % | 9,586 (1.2 KB) | 7 |
| 1,000,000 at 1 % | 9.6 M (1.2 MB) | 7 |
| 1,000,000 at 0.1 % | 14.4 M (1.8 MB) | 10 |

The same idea appears in CDNs (cache a file only on its second request), in browsers (checking
URLs against a malware list) and in databases (skipping partitions in joins). Relatives:
**HyperLogLog** counts distinct items in a few KB, **count-min sketch** estimates frequencies.
You implement a Bloom filter in the next kata.

## Summary

- Pick the data model from the access patterns; put big blobs in object storage.
- B-trees update pages in place (WAL first); LSM trees append and compact.
- Indexes speed reads and cost every write; column order matters in composite indexes.
- Local secondary indexes scatter reads; global ones make writes remote.
- OLTP is row-oriented and key-driven; analytics belongs in a columnar store fed by CDC.
- Bloom filters answer "definitely not" or "probably yes" with ~10 bits per key at 1 %.

## Further reading

- Martin Kleppmann, *Designing Data-Intensive Applications*, chapters 2–3: <https://dataintensive.net/>
- RocksDB overview (LSM, compaction, Bloom filters): <https://github.com/facebook/rocksdb/wiki/RocksDB-Overview>
- PostgreSQL documentation, "B-Tree Indexes": <https://www.postgresql.org/docs/current/btree.html>
- Markus Winand, *Use The Index, Luke*: <https://use-the-index-luke.com/>
- Wikipedia, "Bloom filter": <https://en.wikipedia.org/wiki/Bloom_filter>
- Amazon S3 user guide (object storage concepts): <https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html>
