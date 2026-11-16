---
from: python
status: reviewed
concept: "A log that everything else is built on"
from_way: "Append-only logs and journals, a Redis replica for reads, and Celery consumers that acknowledge messages."
native_way: "The WAL is the database's journal: replicas replay it, slots remember how far each consumer got, and failover promotes a standby."
sensei_tip: "Routing reads to a standby (Django database routers, SQLAlchemy binds) means reading slightly old data: after a write, read from the primary or use synchronous_commit = remote_apply."
---

| Python | PostgreSQL |
| --- | --- |
| an append-only journal file | the WAL |
| a file offset | an LSN |
| a Django database router for reads | queries on a hot standby |
| a message acknowledged by a consumer | WAL confirmed by a replication slot |
| file.flush(); os.fsync() | the WAL flush at COMMIT |
