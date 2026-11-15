---
from: python
status: draft
concept: "Operating what you deployed"
from_way: "requirements.txt or uv.lock, logging.handlers.RotatingFileHandler, APScheduler or Celery beat, and the hosting platform doing the rest."
native_way: "The system-level versions: apt holds, logrotate, cron or timers, backups with restore tests and servers as code."
sensei_tip: "A scheduled job inside a web app runs once per worker and per replica. Move it to a systemd timer or a Kubernetes CronJob, or protect it with a lock."
---

| Python | Linux |
| --- | --- |
| pip freeze / uv.lock | apt-mark hold, pinned packages |
| RotatingFileHandler | logrotate, or stdout plus the journal |
| Celery beat | a crontab line or a systemd timer |
| Django migrations | Ansible or cloud-init in the repository |
| pg_dump in a cron job | pg_dump plus restic and a restore test |
