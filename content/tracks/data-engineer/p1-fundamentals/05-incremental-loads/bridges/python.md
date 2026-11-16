---
from: python
status: draft
concept: "Safe, repeatable loads"
from_way: "df.to_sql('orders', if_exists='append') after filtering on the last run time."
native_way: "A watermark from the data, row_number() deduplication, and an upsert or partition overwrite."
sensei_tip: "if_exists='append' is a plain INSERT: a rerun duplicates. Load to a staging table, then run one INSERT ... ON CONFLICT or MERGE in the same transaction."
---

| Python | Incremental loads |
| --- | --- |
| to_sql(if_exists='append') | not idempotent |
| to_sql(if_exists='replace') | a full reload |
| df.sort_values(...).drop_duplicates('order_id', keep='last') | deduplication |
| sqlite3 ON CONFLICT DO UPDATE | an upsert |
| max(df.updated_at) | the new watermark |
