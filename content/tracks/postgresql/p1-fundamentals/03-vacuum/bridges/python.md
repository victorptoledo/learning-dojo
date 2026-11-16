---
from: python
status: reviewed
concept: "Garbage collection for row versions"
from_way: "Reference counting plus the cyclic gc module, which frees objects when nothing points to them."
native_way: "VACUUM frees row versions once no snapshot can see them; autovacuum decides when, from thresholds per table."
sensei_tip: "A Celery task or a notebook that opens a transaction and forgets it is the database version of a reference cycle nobody collects. Set idle_in_transaction_session_timeout."
---

| Python | PostgreSQL |
| --- | --- |
| gc.collect() | VACUUM |
| gc.set_threshold(...) | autovacuum thresholds and scale factors |
| a lingering reference | a snapshot holding back cleanup |
| tracemalloc | pg_stat_user_tables.n_dead_tup, pgstattuple |
| rewriting a list to free memory | VACUUM FULL or pg_repack |
