---
from: javascript
status: draft
concept: "Running an application as a service"
from_way: "pm2 to keep Node alive, restart it and rotate its logs, or forever and nodemon."
native_way: "systemd does what pm2 does, built into the system: restarts, logs, limits and boot start."
sensei_tip: "With systemd you do not need pm2: ExecStart=/usr/bin/node /srv/app/server.js, Restart=on-failure and the journal cover it. Use Node's cluster or several units for more processes."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| pm2 start server.js | systemctl start app |
| pm2 startup | systemctl enable app |
| pm2 logs | journalctl -u app -f |
| pm2 restart on crash | Restart=on-failure |
| node-cron | a .timer unit |
