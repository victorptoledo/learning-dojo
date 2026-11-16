---
from: python
status: reviewed
concept: "Going back in time after a mistake"
from_way: "manage.py dumpdata fixtures, Alembic downgrade scripts and copying the SQLite file."
native_way: "pg_dump for portable logical copies, base backups plus archived WAL for recovery to any moment."
sensei_tip: "dumpdata and loaddata are not backups for production data: they go row by row through the ORM, are slow, and give no point-in-time recovery. Use pg_dump -Fc for copies and pgBackRest or the provider for real backups."
---

| Python | PostgreSQL |
| --- | --- |
| manage.py dumpdata | pg_dump |
| manage.py loaddata | pg_restore |
| alembic downgrade | restoring a table from a backup |
| shutil.copy of a SQLite file | a base backup (plus WAL to make it consistent) |
| a pytest job that restores and checks | an automated restore test |
