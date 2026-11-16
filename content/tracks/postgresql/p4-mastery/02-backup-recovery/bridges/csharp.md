---
from: csharp
status: reviewed
concept: "Going back in time after a mistake"
from_way: "SQL Server full, differential and log backups, and RESTORE ... WITH STOPAT."
native_way: "Base backups (full, differential or incremental with pgBackRest) plus archived WAL, restored with recovery_target_time."
sensei_tip: "The mapping is close: a full backup is a base backup, log backups are archived WAL segments, and STOPAT is recovery_target_time. The difference is that PostgreSQL restores the whole cluster, not one database."
---

| SQL Server | PostgreSQL |
| --- | --- |
| BACKUP DATABASE | pg_basebackup or pgBackRest full backup |
| BACKUP LOG | continuous WAL archiving |
| RESTORE ... WITH STOPAT | recovery_target_time |
| a marked transaction | pg_create_restore_point |
| BACPAC / generate scripts | pg_dump |
| in-place upgrade | pg_upgrade |
