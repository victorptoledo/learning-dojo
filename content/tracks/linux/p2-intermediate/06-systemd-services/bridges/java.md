---
from: java
status: draft
concept: "Running an application as a service"
from_way: "A Windows service wrapper (procrun, NSSM) or a scheduled task, logs written to files by Logback, and a JVM that must restart itself."
native_way: "A short unit file: systemd starts the JVM as its own user, restarts it on failure, sends SIGTERM to stop it, keeps its stdout in the journal and can sandbox it."
sensei_tip: "Spring Boot works well with Type=simple and logging to the console. Set TimeoutStopSec a little above server.shutdown's graceful period, so in-flight requests finish before SIGKILL."
---

| Windows / Java | systemd |
| --- | --- |
| Windows service wrapper | `/etc/systemd/system/app.service` |
| Startup type "Automatic" | `systemctl enable app` |
| Recovery: restart the service | `Restart=on-failure`, `RestartSec=5` |
| Logback file appender | stdout plus `journalctl -u app` |
| Task Scheduler | a `.timer` unit with `OnCalendar=` |
| Service account | `User=app` or `DynamicUser=yes` |
