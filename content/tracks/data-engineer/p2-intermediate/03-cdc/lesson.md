# Change Data Capture

Incremental loads on `updated_at` cannot see deletes, miss intermediate states and need the
source to keep that column honest. **Change data capture** (CDC) instead records **every**
insert, update and delete as it happens, in commit order, and streams those changes to
other systems.

## Two kinds of CDC

| | Query-based | Log-based |
| --- | --- | --- |
| How | poll the table: `WHERE updated_at > :watermark` | read the database's transaction log |
| Sees deletes | no (unless soft deletes) | yes |
| Sees every intermediate update | no, only the latest state at poll time | yes |
| Load on the source | repeated queries | low: the log is written anyway |
| Needs | an indexed change column | access to the log (replication privileges, configuration) |

## Reading the log

Every serious database writes changes to a log before applying them: PostgreSQL's
**write-ahead log** (WAL), MySQL's binlog, SQL Server's transaction log, MongoDB's oplog.
In PostgreSQL, **logical replication** decodes the WAL into row changes:

```sql
-- on the source (wal_level = logical)
CREATE PUBLICATION shop_cdc FOR TABLE orders, customers;
```

A consumer attaches through a **replication slot**, which remembers the last log position
(the **LSN**) it confirmed. The database keeps WAL until the slot confirms it, so a crashed
consumer resumes exactly where it stopped. The flip side: a consumer that stays down makes
WAL pile up until the disk fills. Monitor slot lag.

## Debezium change events

**Debezium** is the most common open-source CDC platform: connectors for PostgreSQL, MySQL,
SQL Server, MongoDB and others, usually publishing to Kafka (one topic per table). A change
event looks like this (simplified):

```json
{
  "op": "u",
  "before": {"id": 7, "status": "placed", "amount": "19.90"},
  "after":  {"id": 7, "status": "paid",   "amount": "19.90"},
  "source": {"table": "orders", "lsn": 2471934120, "txId": 98812, "ts_ms": 1763190000123},
  "ts_ms": 1763190000456
}
```

`op` is `c` (create), `u` (update), `d` (delete, with `after` null) or `r` (read: a row from
the initial snapshot). Deletes may be followed by a **tombstone** (a message with a null
value) so that Kafka log compaction can drop the key.

## Snapshot, then stream

The log does not contain the past forever, so a CDC pipeline starts with an **initial
snapshot** of the current table contents (`op = r`), noting the log position at which it was
taken, and then streams changes from that position. Debezium does this for you; with a
hand-made pipeline, getting the hand-over between snapshot and stream right is the hard part.

## Applying changes downstream

Two shapes are useful, often both:

1. **A replica of the current state**: upsert on `c`, `u` and `r`, delete on `d`.
   Events can be redelivered (at-least-once), so apply them **idempotently** and use the
   source position (LSN, or `txId` plus order) to ignore an event older than what the row
   already reflects.
2. **A change log**: append every event to a raw table. It gives the full history (every
   status change with its time), feeds type 2 dimensions, and lets you rebuild the replica.

```sql
MERGE INTO orders_replica AS t
USING latest_events AS e ON t.id = e.id
WHEN MATCHED AND e.op = 'd' AND e.lsn > t.lsn THEN DELETE
WHEN MATCHED AND e.op <> 'd' AND e.lsn > t.lsn THEN
    UPDATE SET status = e.status, amount = e.amount, lsn = e.lsn
WHEN NOT MATCHED AND e.op <> 'd' THEN
    INSERT (id, status, amount, lsn) VALUES (e.id, e.status, e.amount, e.lsn);
```

## The outbox pattern

CDC of internal tables couples consumers to the source's schema: rename a column and the
warehouse breaks. When a service wants to publish **domain events** (`OrderPaid`), it can
write them to an **outbox** table in the **same transaction** as the business change, and
CDC publishes the outbox rows. That avoids the dual write (database commit plus a separate
broker publish, where one can fail), and the event is a deliberate, versioned contract.

## Summary

- Log-based CDC sees every insert, update and delete in commit order, at low cost.
- PostgreSQL: logical replication, publications and replication slots; watch slot lag.
- Debezium events carry `op`, `before`, `after` and the source position.
- Start with a snapshot, then stream; apply idempotently, ordered by source position.
- Keep the change log for history; use an outbox for events meant for other teams.

## Further reading

- Debezium documentation, PostgreSQL connector and event format: <https://debezium.io/documentation/reference/stable/connectors/postgresql.html>
- PostgreSQL documentation, logical replication: <https://www.postgresql.org/docs/17/logical-replication.html>
- PostgreSQL documentation, replication slots: <https://www.postgresql.org/docs/17/warm-standby.html#STREAMING-REPLICATION-SLOTS>
- Chris Richardson, "Pattern: Transactional outbox": <https://microservices.io/patterns/data/transactional-outbox.html>
- Martin Kleppmann, *Designing Data-Intensive Applications*, chapter 11 (change data capture and event sourcing)
