---
from: javascript
status: draft
concept: "Safe, repeatable loads"
from_way: "A script that fetches records 'since the last run' and inserts them."
native_way: "Persist a cursor or watermark from the data and upsert by key, so retries are harmless."
sensei_tip: "API pagination cursors are watermarks: store the cursor the API gave you after the batch is saved, not before, and make the save an upsert."
---

| JavaScript / TypeScript | Incremental loads |
| --- | --- |
| ?updated_since= query parameter | a watermark |
| a pagination cursor | a watermark the source gives you |
| prisma.upsert | an idempotent write, row by row |
| createMany({ skipDuplicates }) | insert ignoring duplicates |
| a Map keyed by id | deduplication before writing |
