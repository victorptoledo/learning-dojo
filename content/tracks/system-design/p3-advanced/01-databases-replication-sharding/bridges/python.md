---
from: python
status: draft
concept: "Copies and parts of the data"
from_way: "One DATABASES['default'] in Django and auto-increment ids."
native_way: "A leader and replicas with lag to manage, and shards chosen by a partition key, with routing in the application or a proxy."
sensei_tip: "Django database routers (db_for_read, db_for_write) implement leader/replica routing; pin a user's reads to the leader for a few seconds after a write to keep read-your-writes."
---

| Python | Replication and sharding concept |
| --- | --- |
| Django `DATABASE_ROUTERS` | routing reads, writes and shards |
| `using('replica')` | an explicit replica read |
| `uuid.uuid7()` (Python 3.14) | time-ordered ids without a central sequence |
| SQLAlchemy `horizontal_shard` | sharding in the ORM |
| psycopg `target_session_attrs=read-write` | finding the current leader |
