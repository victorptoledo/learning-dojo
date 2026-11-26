# Checkpoint 3: A Lakehouse Table and an Exactly-Once Stream Sink

The partitioned lake of this phase had two weak spots: a reader could see a partition half
compacted, and nothing said which files belonged to the table. Table formats such as Delta
Lake and Iceberg fix both with a **transaction log**. Build a tiny one in `table.py`, then
use it in `sink.py` to move order events from a Kafka-like topic into the table **exactly
once**, by committing the consumer's offsets in the same commit as the rows.
`topic.py` (the topic) is given; do not change it.

Rows always have the columns of `SCHEMA`: `event_id`, `user_id`, `event_date` (the
partition, `YYYY-MM-DD`) and `amount_cents` (the column with file statistics).

## The log

Version `n` of the table is the file `_log/{n:020d}.json` (`version()` and `_log_path` are
written). A commit is a few **actions**, one JSON object per line:

```json
{"add": {"path": "event_date=2026-11-01/3f2a....parquet", "partition": "2026-11-01", "rows": 2, "min": 500, "max": 990}}
{"remove": {"path": "event_date=2026-11-01/9c1b....parquet"}}
{"txn": {"app_id": "orders", "offsets": {"0": 3, "1": 2}}}
```

A snapshot is the result of replaying versions `0..n` in order: `add` makes a file part of
the table, `remove` takes it out, and the last `txn` of each `app_id` holds its offsets.
Data files on disk that no snapshot adds do not exist for readers.

## `table.py`

| Method | Does |
| --- | --- |
| `commit(actions, read_version)` | writes version `read_version + 1` and returns it. If that version already exists (another writer won), raise `CommitConflict` and leave the winner's file alone. A `read_version` above the latest is a `ValueError` |
| `files(version=None)` | the `add` entries of the snapshot (latest by default), as dicts sorted by `path`. A version that does not exist is a `ValueError` |
| `offsets(app_id, version=None)` | that application's last offsets as `{partition: next offset}`, with **integer** keys; `{}` if it never committed |
| `append(rows, txn=None)` | writes one **new** Parquet file per partition value (Hive-style folder `event_date=<value>/`, a unique file name, without the partition column) and commits their `add` actions with `rows`, `min` and `max` of `amount_cents`. `txn=(app_id, offsets)` adds a `txn` action **to the same commit**. Returns the version; with no rows and no `txn` it commits nothing and returns `None` |
| `scan(version=None, *, dates=None, min_amount=None, columns=None)` | the rows of the snapshot, sorted by `event_id`, in `SCHEMA` order (or only `columns`, in that order), and the list of paths it opened. `dates=(first, last)` skips files of other partitions; `min_amount` skips files whose `max` is below it, then keeps only rows with `amount_cents >= min_amount` |
| `compact(partition)` | rewrites the partition's files as **one** file sorted by `event_id`, in one commit that removes the old files and adds the new one. Fewer than two files: no commit, `None` |
| `vacuum(oldest_version)` | deletes every `.parquet` file under the table that no snapshot from `oldest_version` to the latest uses (removed files and orphans of crashed writers) and returns their paths, sorted. Snapshots before `oldest_version` stop being readable; that is the price of storage |

Old files stay on disk after `compact`, so `scan(version=...)` still reads old snapshots
(**time travel**).

## `sink.py`

**`event_date(event_time)`** is the **UTC** date of an ISO 8601 time with an offset
(`2026-11-01T22:30:00-03:00` is `2026-11-02`).

**`run_batch(topic, table, app_id, max_records=100)`** moves one micro-batch:

1. Read the committed offsets of `app_id` **from the table** (0 for a partition never read).
2. Read up to `max_records` records from each partition, from its offset.
3. Turn each event (`event_id`, `user_id`, `event_time`, `amount_cents`) into a row in the
   partition of its **event time**, even when it arrives late. Skip an event whose
   `event_id` is already in the table or earlier in the batch: producers retry.
4. Append the rows with `txn=(app_id, offsets)`, where `offsets` holds the next offset of
   **every** partition of the topic.

Return the number of rows written. When no partition had a new record, commit nothing and
return 0; a batch of only duplicates still commits, so its offsets move on.

Because rows and offsets land in one commit, a crash before the commit leaves both
untouched and the next batch reads the same records again: no loss and no duplicates.

**`lag(topic, table, app_id)`** is `{partition: end offset - committed offset}` for every
partition, the first number to watch on a streaming pipeline.

> [!TIP]
> A file opened with mode `"x"` refuses to overwrite, which gives you `CommitConflict`.
> Real table formats also make the new version appear complete or not at all: write a
> temporary file and then `os.link` it to the version's name, or rename it with an
> operation that never replaces a file.

Run the visible tests with `uv run pytest`. There are 37 tests: 2 in
`tests/test_visible.py` and 32 hidden ones, plus ruff on `table.py`, `sink.py` and
`topic.py`.
