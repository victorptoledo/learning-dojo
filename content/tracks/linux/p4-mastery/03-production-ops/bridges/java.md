---
from: java
status: draft
concept: "Operating what you deployed"
from_way: "Dependency versions pinned in the Maven pom, logs rotated by Logback's rolling appender, @Scheduled jobs inside the application, and a database backup the DBA takes care of."
native_way: "The same concerns at the system level: apt pins and holds, logrotate, cron or timers, restic or borg backups, and the whole server described as code."
sensei_tip: "A @Scheduled job runs once per instance: with three replicas it runs three times. For jobs that must run once, use a systemd timer on one machine, a Kubernetes CronJob, or a lock such as ShedLock."
---

| Java habit | Linux operations |
| --- | --- |
| versions pinned in the `pom.xml` | `apt-mark hold`, pinned package versions |
| Logback `RollingFileAppender` | `logrotate`, or stdout plus the journal |
| `@Scheduled(cron = "0 30 2 * * *")` | a crontab line or a systemd timer |
| Flyway migrations in the repository | Ansible playbooks or cloud-init in the repository |
| "the DBA backs it up" | `pg_dump` plus restic, and a restore test you run |
