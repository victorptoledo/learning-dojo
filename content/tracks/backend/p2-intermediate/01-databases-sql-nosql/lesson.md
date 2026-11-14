# Transactions, Isolation and NoSQL

Two requests touch the same data at the same time. One reads a balance while another withdraws;
two clerks book the last seat. Databases give you **transactions** to keep such races from
corrupting data, but the guarantees depend on the **isolation level**, and the cost of stronger
guarantees is real. This lesson covers both, and when a non-relational store is the better tool.

## ACID

| Letter | Promise |
| --- | --- |
| **Atomicity** | all the statements of a transaction happen, or none does |
| **Consistency** | constraints (keys, `check`, `unique`) hold after every transaction |
| **Isolation** | concurrent transactions do not see each other's half-done work (to a degree) |
| **Durability** | once committed, it survives a crash |

## Isolation levels and their anomalies

Full isolation would run transactions one after the other, which is slow. SQL defines levels that
allow some **anomalies** in exchange for concurrency:

| Anomaly | What happens |
| --- | --- |
| **Dirty read** | you read a change another transaction has not committed (and may roll back) |
| **Non-repeatable read** | you read a row twice and get different values, because someone committed in between |
| **Phantom** | you run the same query twice and new rows appear |
| **Lost update** | two transactions read the same value, both change it, the second overwrites the first |
| **Write skew** | two transactions each check a condition, both pass, together they break it (two doctors both go off call) |

| Level | Prevents | Default in |
| --- | --- | --- |
| Read uncommitted | (nothing in practice) | |
| **Read committed** | dirty reads | PostgreSQL, Oracle, SQL Server, H2 |
| **Repeatable read** | + non-repeatable reads (in PostgreSQL also phantoms) | MySQL InnoDB |
| **Serializable** | everything: as if transactions ran one at a time | |

Most applications run at **read committed** and must handle lost updates and write skew
explicitly. Under **serializable**, the database aborts one of two conflicting transactions with a
serialization error, and the application must **retry** it.

## How databases do it: locks and MVCC

PostgreSQL, Oracle and InnoDB use **MVCC** (multi-version concurrency control): writers create new
row versions, readers see the version that was current for them, so **readers do not block writers**
and writers do not block readers. Writers on the same row still wait for each other.

## Preventing lost updates

Three tools, from the database's side:

```sql
-- 1. Atomic update: let the database do the arithmetic
update accounts set balance = balance - 100 where id = 7 and balance >= 100;
-- 0 rows updated means "not enough money": no read-then-write race

-- 2. Pessimistic lock: hold the row while you decide
select * from seats where id = 12 for update;   -- others wait until you commit
update seats set booked_by = 'ada' where id = 12;

-- 3. Optimistic lock: detect the conflict at write time
update issues set title = 'new', version = version + 1 where id = 31 and version = 4;
-- 0 rows: someone else changed it; reload and retry (or answer 409/412)
```

| | Optimistic (version column, `@Version`) | Pessimistic (`select ... for update`) |
| --- | --- | --- |
| Cost when there is no conflict | none | holds a lock |
| When conflicts are frequent | many retries | waits, no wasted work |
| Fits | web apps, long "think time" between read and write | short, hot transactions (inventory, seats) |
| Risk | retries storms | deadlocks; keep lock order consistent |

Constraints are the backstop: a `unique` index makes two concurrent "check then insert" requests
fail cleanly instead of creating duplicates.

## Transactions in application code

- Keep transactions **short**: no remote calls, no waiting for users inside them.
- Know where they start and end (`@Transactional` on the service method).
- Treat a serialization failure or an optimistic-lock conflict as **retryable**, a constraint
  violation as a **client error** (`409`).

## NoSQL: other shapes of data

"NoSQL" groups stores that drop the relational model to gain something else.

| Family | Examples | Strong at | Weak at |
| --- | --- | --- | --- |
| **Key-value** | Redis, DynamoDB (as KV) | very fast get/set by key, caches, counters, sessions | queries beyond the key |
| **Document** | MongoDB, Couchbase | nested JSON documents read and written as a whole | joins, cross-document transactions |
| **Wide-column** | Cassandra, ScyllaDB | huge write volumes, time series, multi-region | ad-hoc queries; model by query |
| **Graph** | Neo4j | relationship-heavy queries (paths, recommendations) | bulk analytics |
| **Search** | Elasticsearch, OpenSearch | full-text search, faceting | being the source of truth |

How to choose:

- Start with a **relational database**. It handles most workloads, gives constraints and
  transactions, and PostgreSQL's `jsonb` covers many "we need documents" cases.
- Add a specialised store for a **specific, measured need**: a cache (Redis), search
  (Elasticsearch), a write volume or global distribution a single relational database cannot take.
- In many NoSQL stores you **model by query**: you design the tables or documents around the access
  patterns, often duplicating data, because there are no joins to fix it later.

## Summary

- ACID: atomic, consistent, isolated (to a level), durable.
- Read committed (the usual default) allows non-repeatable reads, phantoms, lost updates and write
  skew; serializable prevents them and requires retries.
- MVCC lets readers and writers proceed without blocking each other.
- Prevent lost updates with atomic `update ... where`, optimistic version checks or `for update`
  locks, and back them with constraints.
- Choose NoSQL families for specific needs; start relational.
