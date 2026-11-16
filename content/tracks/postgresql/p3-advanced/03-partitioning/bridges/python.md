---
from: python
status: reviewed
concept: "One logical table, many physical ones"
from_way: "Splitting data into files or tables per month by hand, and writing to the right one from the script."
native_way: "Declarative partitioning: one table for the application, partitions picked by the database."
sensei_tip: "Django has no built-in partitioning support: create the partitioned table in a RunSQL migration (or use django-postgres-extra) and keep the model unmanaged, with the composite primary key in mind."
---

| Python | PostgreSQL |
| --- | --- |
| TimedRotatingFileHandler | range partitions by time |
| os.remove(old_log) | DETACH PARTITION, then DROP TABLE |
| hash(key) % 8 to pick a shard | HASH partitioning with MODULUS 8 |
| a cron job creating next month's table | pg_partman or a scheduled job |
