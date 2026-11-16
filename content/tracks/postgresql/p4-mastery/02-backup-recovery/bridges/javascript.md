---
from: javascript
status: reviewed
concept: "Going back in time after a mistake"
from_way: "mongodump and mongorestore, or the hosting provider's daily snapshot button."
native_way: "pg_dump for logical copies, and base backups with archived WAL for recovery to any second."
sensei_tip: "Managed providers' point-in-time restore creates a new instance at the chosen time; script it and practise it, then copy the missing rows back instead of switching the app over."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| mongodump | pg_dump |
| mongorestore | pg_restore |
| a daily snapshot | a base backup |
| the oplog | the archived WAL |
| restore to a point in time from the console | PITR with recovery_target_time |
