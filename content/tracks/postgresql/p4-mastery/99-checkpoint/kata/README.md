# Checkpoint 4: Disaster Recovery Drill

Yesterday, 2026-11-25, at 14:30, a clean-up meant for the archive database ran on the payments
primary: `DELETE FROM payments WHERE created_at < '2026-10-01'`. The shop kept working after
that. `fixture/01_drill.sql` creates what you have this morning:

```text
primary_status  (captured_at, current_lsn)                         -- one row, 14:00 yesterday
slots_snapshot  (slot_name, slot_type, active, restart_lsn, wal_status)   -- pg_replication_slots then
base_backups    (label, started_at, finished_at, start_lsn, stop_lsn)    -- finished_at NULL: failed
wal_archive     (segment, archived_at)                              -- the WAL files in the bucket
customers, payments (id identity, customer_id, amount, status, created_at)
refunds         (id identity, payment_id → payments ON DELETE CASCADE, amount, created_at)
restored.payments, restored.refunds                                 -- the PITR copy at 14:29
ledger (id identity, account, amount, at), import_staging (line, raw)
```

| File | Kind | Task |
| --- | --- | --- |
| `slots.sql` | one query | The slots that need attention: those that are **inactive**, those whose `wal_status` is `unreserved` or `lost`, and those that hold back **more than 1 GB** of WAL (1 GB = 1024³ bytes) behind `primary_status.current_lsn`. Return `slot_name`, `slot_type`, `active`, `retained_mb` (the WAL held back, in MB of 1024² bytes, rounded to a whole number; NULL when the slot has no `restart_lsn`), most WAL first, NULLs last, then by name. |
| `restore_plan.sql` | one query | One row for a restore to **2026-11-25 14:29+00**: `backup` (the label of the newest backup that **finished before** the target), `first_segment` (the WAL file holding its `start_lsn`), `last_segment` (the first file archived **at or after** the target: it holds the WAL of 14:29), `segments` (how many files the restore replays, both ends included) and `missing` (how many of them the archive no longer has). |
| `recover.sql` | statements | Put back **exactly** what the accident deleted, from the `restored` copy: the payments it named and the refunds it removed without naming them, with their **original ids** and values. Do not touch a row that exists now (it may have changed since 14:29), and do not bring back rows deleted on purpose later. Running the file twice must change nothing the second time. New payments keep getting fresh ids. |
| `wal_budget.sql` | statements | (1) A function `wal_bytes(statement text) RETURNS numeric` that runs a statement and returns how many bytes of WAL it wrote. (2) The bank's file can be downloaded again at any time: make `import_staging` write (almost) no WAL. |

What you need to know about WAL file names (timeline 1, 16 MB segments):

- A WAL file name has 24 hex digits: the timeline (`00000001`), then the segment number divided
  by 256, then the segment number modulo 256, each in 8 digits. Segment 0x2FF is
  `0000000100000002000000FF`, and the next one is `000000010000000300000000`.
- The segment of an LSN is `floor((lsn - '0/0') / 16 MB)`, and `pg_walfile_name(lsn)` gives its
  name. A file is archived when it is full, so the file archived first at or after a moment holds
  the WAL written at that moment.

What you need to know about WAL positions: `pg_current_wal_insert_lsn()` is where the next record
goes in the WAL buffers; `pg_current_wal_lsn()` (written) and `pg_current_wal_flush_lsn()`
(flushed to disk) trail behind it until the buffers are written out. Subtracting two `pg_lsn`
values gives bytes.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`. Run
`\i recover.sql` inside `BEGIN;` ... `ROLLBACK;` to try it as often as you like.

There are 18 tests: 1 in `tests/visible.sql` and 17 hidden ones.
