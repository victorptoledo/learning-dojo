---
from: java
status: draft
concept: "Safe, repeatable loads"
from_way: "repository.saveAll(batch) after reading rows changed since the last run time."
native_way: "Read after a data watermark, deduplicate, and upsert or MERGE so a retry changes nothing."
sensei_tip: "Spring Batch's restartability (saved execution context) is the same idea as a watermark: persist how far you got only after the chunk commits, in the same transaction."
---

| Java | Incremental loads |
| --- | --- |
| JobExecutionContext saved after a chunk | a watermark stored after success |
| saveAll (insert or update by id) | an upsert, row by row |
| INSERT ... ON CONFLICT via JdbcTemplate | a set-based upsert |
| @Retryable | a retry that must be idempotent |
| a LinkedHashMap by id keeping the latest | deduplication |
