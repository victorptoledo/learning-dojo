---
from: python
status: draft
concept: "Recovery is designed, tested and timed"
from_way: "A cron job runs a backup script and emails when it fails, sometimes."
native_way: "Backups are managed and immutable, a restore test runs every week and checks row counts, and a game day rehearses the failover."
sensei_tip: "Write the restore check in Python: restore, run a few known queries, compare counts and checksums, and alert if anything differs."
---

| Python world | Disaster recovery |
| --- | --- |
| a backup cron script | managed, immutable backups |
| an email on failure | an alert plus a restore test |
| `pytest` fixtures | restore verification checks |
| a chaos script | a controlled experiment |
