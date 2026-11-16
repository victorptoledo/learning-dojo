---
from: go
status: draft
concept: "Safe, repeatable loads"
from_way: "A loop that inserts every row returned since the last run time."
native_way: "Copy the batch into a temporary table, deduplicate, upsert, and store the watermark in the same transaction."
sensei_tip: "With pgx, CopyFrom into a temp table and one INSERT ... SELECT ... ON CONFLICT is both fast and idempotent; keep the watermark update inside the same tx."
---

| Go | Incremental loads |
| --- | --- |
| pgx.CopyFrom into a temp table | landing the batch |
| INSERT ... ON CONFLICT DO UPDATE | an upsert |
| tx.Commit() after the watermark update | atomic progress |
| map[int]Order keeping the latest | deduplication |
| time.Time from the data, not time.Now() | the watermark |
