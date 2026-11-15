---
from: javascript
status: draft
concept: "Operating what you deployed"
from_way: "package-lock.json, pm2's log rotation, node-cron, and a PaaS that hides the server."
native_way: "apt, logrotate, cron or timers, backups and configuration as code when you run the servers yourself."
sensei_tip: "pm2-logrotate and node-cron solve inside Node what the system already does better: let systemd collect stdout and schedule jobs with timers, so they also run when the app is down."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| package-lock.json | apt-mark hold, pinned versions |
| pm2-logrotate | logrotate or the journal |
| node-cron | cron or a systemd timer |
| Heroku release phase | a migration step in the deploy script |
| a PaaS backup add-on | restic or borg with restore tests |
