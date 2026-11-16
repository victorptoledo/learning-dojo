---
from: java
status: reviewed
concept: "Going back in time after a mistake"
from_way: "Git history for code, Flyway migrations with undo scripts that nobody tests, and trusting the cloud provider's snapshots."
native_way: "Base backups plus archived WAL, which let you replay the database to any moment, and pg_dump for portable copies."
sensei_tip: "Treat a restore like a test suite: a scheduled job restores last night's backup to a scratch instance, runs a few queries, and fails loudly. Untested backups fail exactly when you need them."
---

| Java | PostgreSQL |
| --- | --- |
| git checkout <commit> | point-in-time recovery to a target time |
| a tag before a release | pg_create_restore_point('before_release_42') |
| a Flyway undo script | a restore of the table from a backup |
| an integration test | an automated restore test |
| upgrading the JDK with a canary | pg_upgrade rehearsed on a restored copy |
