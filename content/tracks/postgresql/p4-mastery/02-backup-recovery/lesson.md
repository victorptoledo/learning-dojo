# Backups, Point-in-Time Recovery and Upgrades

A replica is not a backup: a `DROP TABLE` or a buggy migration replicates in milliseconds. A
backup is what lets you go back to **before** the mistake. This lesson covers logical dumps,
physical base backups with WAL archiving, point-in-time recovery, the tools people use, how to
prove a backup works, and how major-version upgrades are done.

## Two kinds of backup

| | Logical (`pg_dump`) | Physical (base backup + WAL) |
| --- | --- | --- |
| contains | SQL or an archive of one database's objects and data | a copy of the whole data directory, plus the WAL since |
| restores to | any later major version, another platform, one table | the same major version, the whole cluster |
| point in time | only the moment of the dump | **any moment** covered by the archived WAL |
| speed on 1 TB | hours to dump, hours to restore (indexes rebuilt) | as fast as copying files, plus WAL replay |
| good for | small databases, migrations, copying one schema or table | production disaster recovery |

Most production setups use physical backups with archived WAL, and `pg_dump` for extra copies
of important tables and for moving data between versions.

## Logical backups with pg_dump

```bash
pg_dump -d shop -Fc -f shop.dump                 # custom format: compressed, restorable selectively
pg_dump -d shop -Fd -j 8 -f shop_dir/            # directory format, 8 parallel jobs
pg_dumpall --globals-only -f globals.sql         # roles and tablespaces (pg_dump does not include them)

pg_restore -d shop_restored -j 8 shop.dump                   # restore everything, in parallel
pg_restore -d shop_restored -t invoices shop.dump            # one table
pg_restore -l shop.dump                                      # list the contents
```

`pg_dump` takes a consistent snapshot (REPEATABLE READ) and does not block normal reads and
writes, but it holds `ACCESS SHARE` locks, so a long dump blocks DDL on the dumped tables and
keeps an old snapshot (VACUUM cannot clean up behind it). Plain SQL format (`-Fp`) is readable but
must be replayed with psql and cannot restore in parallel.

## Physical backups and WAL archiving

A physical backup has two parts:

1. A **base backup**: a copy of the data directory taken while the server runs, with
   `pg_basebackup` or a backup tool. Because pages change during the copy, it is inconsistent
   on its own.
2. The **WAL** from the start of the base backup on, archived continuously. Replaying it makes the
   base backup consistent and can carry it forward to any later moment.

```text
archive_mode = on
archive_command = 'pgbackrest --stanza=main archive-push %p'   # or a script that copies %p safely
# 15+: archive_library for modules instead of a shell command
```

`archive_command` must return success **only** when the file is safely stored; if it keeps
failing, WAL accumulates in `pg_wal` (watch `pg_stat_archiver.failed_count` and
`last_failed_wal`). `archive_timeout` forces a segment switch on quiet servers so the archive is
never far behind.

### Point-in-time recovery

To restore to 14:29:59, just before someone ran `DELETE FROM invoices` at 14:30:

1. Restore the last base backup taken before 14:30 into an empty data directory.
2. Configure recovery:

   ```text
   restore_command = 'pgbackrest --stanza=main archive-get %f "%p"'
   recovery_target_time = '2026-11-15 14:29:59+00'
   recovery_target_action = 'pause'      # inspect before opening it for writes
   ```

3. Create `recovery.signal` and start the server. It replays WAL up to the target and pauses;
   check the data, then `SELECT pg_wal_replay_resume()` (or promote).

Targets can also be a transaction id (`recovery_target_xid`), an LSN, or a named restore point
(`SELECT pg_create_restore_point('before_migration_42')`). Restore to a **separate** server,
then copy the lost rows back, rather than rolling the whole production database back.

### The tools

Writing this by hand is error-prone. Use a dedicated tool:

| Tool | Notes |
| --- | --- |
| **pgBackRest** | full, differential and incremental backups, parallel and compressed, WAL archiving, retention, encryption, S3/Azure/GCS, verification |
| **Barman** | backups and WAL archiving managed from a central server |
| **WAL-G** | backups and WAL to object storage, popular in Kubernetes |
| `pg_basebackup` | built in; full backups only (incremental backups with `pg_combinebackup` since 17) |
| managed services | automatic snapshots plus WAL, PITR from the console |

## RPO, RTO and the only backup that counts

| Term | Question | Driven by |
| --- | --- | --- |
| **RPO** (recovery point objective) | how much data may we lose? | how often WAL is archived (seconds) or dumps are taken (hours) |
| **RTO** (recovery time objective) | how long may we be down? | restore speed: base backup size, WAL to replay, incremental backups, a warm standby |

A backup you have never restored is a hope, not a backup. Schedule **restore tests**: restore the
latest backup to a scratch server automatically, start it, run checks (row counts, a known recent
row, `amcheck` on indexes), and alert when it fails. Keep backups in another account or region
than the database, encrypted, with retention long enough to notice a slow corruption.

Data checksums (`initdb --data-checksums`, or `pg_checksums --enable` on a stopped cluster)
detect corrupted pages on read, so you learn about disk problems before your backups inherit
them.

## Major-version upgrades

Minor upgrades (17.2 → 17.4) are a binary swap and a restart. Major upgrades change the on-disk
format:

| Method | Downtime | Notes |
| --- | --- | --- |
| `pg_dump` + `pg_restore` | the whole dump and restore | simplest; only for small databases |
| `pg_upgrade` | minutes (`--link` or `--clone` mode reuses the data files) | rebuilds the catalog only; run `ANALYZE` afterwards (statistics are not copied before 18); test with `--check` first |
| logical replication | seconds (the switchover) | set up the new version as a subscriber, let it catch up, switch the application; handle DDL and sequences |

Before any major upgrade: read the release notes' incompatibilities, check that every extension
exists for the new version, upgrade a restored copy first and run the application's tests against
it, and have the rollback plan (the old cluster untouched, or a fresh backup).

## Summary

- Replicas protect against hardware failure; only backups protect against mistakes.
- `pg_dump` (custom or directory format, `pg_restore -j`) for small databases, single tables and
  cross-version moves; `pg_dumpall --globals-only` for roles.
- Physical base backups plus continuous WAL archiving allow point-in-time recovery to any
  moment; restore to a separate server and pause at the target.
- Use pgBackRest, Barman, WAL-G or the managed service; monitor `pg_stat_archiver`.
- Define RPO and RTO, and test restores automatically.
- Upgrade majors with `pg_upgrade --link` (then `ANALYZE`) or logical replication, after
  rehearsing on a copy.

## Further reading

- PostgreSQL 17, "Backup and Restore": <https://www.postgresql.org/docs/17/backup.html>
- PostgreSQL 17, "Continuous Archiving and Point-in-Time Recovery": <https://www.postgresql.org/docs/17/continuous-archiving.html>
- PostgreSQL 17, "pg_dump" and "pg_restore": <https://www.postgresql.org/docs/17/app-pgdump.html>
- PostgreSQL 17, "Recovery Target" settings: <https://www.postgresql.org/docs/17/runtime-config-wal.html#RUNTIME-CONFIG-WAL-RECOVERY-TARGET>
- PostgreSQL 17, "pg_upgrade": <https://www.postgresql.org/docs/17/pgupgrade.html>
- PostgreSQL 17, "Upgrading a PostgreSQL Cluster": <https://www.postgresql.org/docs/17/upgrading.html>
- pgBackRest user guide: <https://pgbackrest.org/user-guide.html>
