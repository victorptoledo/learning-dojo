# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): the backup script a systemd timer would run
   every night: options, atomic writes, rotation of old archives and a dry run.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Bash Scripting | Why quote `"$@"`? What does `set -euo pipefail` catch and miss? How does `getopts` report a missing value? |
| Text Processing | Basic or extended regex? How does awk group by a field? How do I pass a shell value into awk? |
| Processes and Signals | What is the difference between TERM and KILL? Why `sleep & wait`? What does status 137 mean? |
| systemd | Where do my units go? What is `TimeoutStopSec`? Why a timer instead of cron? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
