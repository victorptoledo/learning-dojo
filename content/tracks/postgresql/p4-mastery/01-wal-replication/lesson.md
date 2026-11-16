# WAL, Replication and High Availability

Everything in this phase rests on one mechanism: the **write-ahead log**. It makes commits
durable, makes crash recovery possible, feeds replicas, and is what point-in-time recovery
replays. This lesson explains the WAL and checkpoints, then physical and logical replication,
replication slots, synchronous commit, and how failover is done in practice.

## The WAL and checkpoints

Every change (to a table, an index, the catalog) is first described in a **WAL record** and
appended to the log in `pg_wal/`, in 16 MB **segment** files. A position in the log is an
**LSN** (log sequence number), shown as `0/3A4F2C8`:

```sql
SELECT pg_current_wal_lsn();                                       -- where the log is now
SELECT pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), '0/3000000'));   -- bytes between two LSNs
SELECT pg_walfile_name(pg_current_wal_lsn());                      -- the segment file
```

At `COMMIT`, the WAL up to the commit record is flushed to disk (`fsync`); the changed data pages
stay in shared buffers and are written later. A **checkpoint** writes all dirty pages and records
a point from which crash recovery can start; WAL before it is no longer needed for recovery and
can be recycled.

| Setting | Role | Typical |
| --- | --- | --- |
| `checkpoint_timeout` | at most this long between checkpoints | 15 min |
| `max_wal_size` | a checkpoint starts early when this much WAL accumulates | a few GB, sized so most checkpoints are by time |
| `checkpoint_completion_target` | spread the writes over this fraction of the interval | 0.9 (default) |
| `wal_level` | how much is logged: `minimal`, `replica` (default), `logical` | `logical` if you use logical replication |
| `synchronous_commit` | whether `COMMIT` waits for the WAL flush (and replicas) | `on` |
| `full_page_writes` | after a checkpoint, the first change to a page logs the whole page | `on`: protects against torn pages |

More checkpoints mean more full-page writes and more WAL; fewer mean a longer crash recovery.
`log_checkpoints` shows whether checkpoints are "time" or "wal" triggered.

`synchronous_commit = off` (per session or transaction) lets `COMMIT` return before the WAL is
flushed: much faster for bulk work, at the risk of losing the last few hundred milliseconds of
commits in a crash, **without** corrupting anything. `fsync = off` is different: it can corrupt
the cluster and is only for throwaway databases (the dojo's test servers use it).

## Physical (streaming) replication

A **standby** is a byte-for-byte copy of the primary that replays its WAL continuously:

```text
primary ── WAL sender ──── stream of WAL ────▶ WAL receiver ── startup process replays ── standby
                                                                (read-only queries: hot standby)
```

Setting one up, in outline:

```bash
# on the standby host, with an empty data directory
pg_basebackup -h primary -U replicator -D /var/lib/postgresql/17/main -X stream -R -S standby1 -C
#   -X stream  copy the WAL needed during the backup
#   -R         write primary_conninfo and create standby.signal
#   -S/-C      create and use the replication slot standby1
```

The standby starts, connects as a role with `REPLICATION` (allowed in `pg_hba.conf` for the
`replication` database), and streams. `hot_standby = on` (default) allows read-only queries on
it.

```sql
-- on the primary
SELECT client_addr, state, sent_lsn, replay_lsn,
       pg_size_pretty(pg_wal_lsn_diff(sent_lsn, replay_lsn)) AS replay_lag_bytes,
       write_lag, flush_lag, replay_lag, sync_state
FROM pg_stat_replication;

-- on the standby
SELECT pg_is_in_recovery(), pg_last_wal_replay_lsn(), now() - pg_last_xact_replay_timestamp() AS lag;
```

Physical replication copies **everything** (all databases, every index, bloat included) and the
standby must run the same major version on the same platform.

### Replica conflicts

A query on the standby may need row versions that the primary's VACUUM already removed. The
standby then waits up to `max_standby_streaming_delay` (30 s) and cancels the query ("canceling
statement due to conflict with recovery"). `hot_standby_feedback = on` tells the primary about
the standby's oldest snapshot, avoiding the cancels at the cost of bloat on the primary (lesson
[VACUUM](../../p1-fundamentals/03-vacuum/lesson.md)).

## Replication slots

Without a slot, the primary recycles WAL after checkpoints, and a standby that falls behind too far
can never catch up. A **replication slot** makes the primary keep WAL until the slot's consumer
has received it:

```sql
SELECT slot_name, slot_type, active, wal_status,
       pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS retained_wal
FROM pg_replication_slots;
```

The danger: an **abandoned slot** (a decommissioned standby, a CDC tool that stopped) keeps WAL
forever, until `pg_wal` fills the disk and the primary stops. Always monitor retained WAL, set
`max_slot_wal_keep_size` to cap it, and drop slots nobody uses:
`SELECT pg_drop_replication_slot('old_standby');`.

## Synchronous replication

By default replication is **asynchronous**: a commit returns once the primary flushed it, and a
standby may be a little behind. If the primary dies, the last commits may be missing on the
standby. With synchronous replication, commits wait for standbys:

```text
synchronous_standby_names = 'ANY 1 (standby_a, standby_b)'   # wait for one of the two
synchronous_commit = on            # wait until the standby has flushed the WAL
                   = remote_apply  # ... and applied it (read-your-writes on the standby)
                   = remote_write  # ... only written to its OS
```

Each commit now pays a network round trip, and if no synchronous standby is reachable, commits
**hang**. Use `ANY 1 (...)` with at least two standbys, and choose per transaction:
`SET LOCAL synchronous_commit = local` for unimportant writes.

## Logical replication

Logical replication sends **row changes** (inserts, updates, deletes) of chosen tables, decoded
from the WAL, instead of physical pages:

```sql
-- on the source (wal_level = logical)
CREATE PUBLICATION shop_pub FOR TABLE orders, customers;
-- on the target, which already has the tables
CREATE SUBSCRIPTION shop_sub CONNECTION 'host=source dbname=shop user=replicator'
    PUBLICATION shop_pub;
```

| | Physical | Logical |
| --- | --- | --- |
| copies | the whole cluster, byte for byte | chosen tables, row by row |
| versions | same major version | can differ: the basis of near-zero-downtime upgrades |
| target | read-only standby | a normal writable database |
| replicates DDL, sequences | yes | **no**: apply schema changes on both sides yourself, and set sequences on the target before switching over |
| needs | a primary key or replica identity | a primary key (or `REPLICA IDENTITY FULL`) to update and delete |

It also powers change data capture: Debezium and similar tools read a logical slot and publish the
changes to Kafka.

## High availability and failover

Replication gives you a copy; **failover** turns a standby into the new primary:

```sql
SELECT pg_promote();      -- on the standby (or pg_ctl promote)
```

Doing it by hand is slow and risky: you must make sure the old primary is really dead (or
fenced), so two primaries never accept writes (**split brain**), then point the applications at
the new one. In production this is automated by a cluster manager such as **Patroni** (with etcd,
Consul or Kubernetes as the source of truth for who the leader is), `pg_auto_failover`, or the
Kubernetes operators (CloudNativePG, Crunchy PGO), or by the managed service. Clients find the
primary through a virtual IP, DNS, a proxy (HAProxy, PgBouncer) or libpq's multi-host
connection strings with `target_session_attrs=read-write`.

After a failover, the old primary must be rebuilt as a standby (`pg_rewind` can resynchronise it
without a full copy, if `wal_log_hints` or data checksums were on).

## Summary

- Every change goes to the WAL first; checkpoints write the pages and bound crash recovery;
  size `max_wal_size` so checkpoints happen by time.
- `synchronous_commit = off` risks losing recent commits, never consistency; `fsync = off` risks
  corruption.
- Streaming replication keeps a read-only standby with the same major version; watch
  `pg_stat_replication` lag and replica query conflicts.
- Replication slots retain WAL; an abandoned slot fills the disk: monitor and cap it.
- Synchronous replication trades commit latency for no data loss; use `ANY 1` of two or more.
- Logical replication copies chosen tables between versions; it skips DDL and sequences.
- Automate failover (Patroni, operators, managed services); avoid split brain.

## Further reading

- PostgreSQL 17, "Reliability and the Write-Ahead Log": <https://www.postgresql.org/docs/17/wal.html>
- PostgreSQL 17, "WAL Configuration" (checkpoints): <https://www.postgresql.org/docs/17/wal-configuration.html>
- PostgreSQL 17, "High Availability, Load Balancing, and Replication": <https://www.postgresql.org/docs/17/high-availability.html>
- PostgreSQL 17, "Log-Shipping Standby Servers" (streaming, slots, synchronous replication): <https://www.postgresql.org/docs/17/warm-standby.html>
- PostgreSQL 17, "Logical Replication": <https://www.postgresql.org/docs/17/logical-replication.html>
- PostgreSQL 17, "pg_basebackup": <https://www.postgresql.org/docs/17/app-pgbasebackup.html>
- Patroni documentation: <https://patroni.readthedocs.io/>
