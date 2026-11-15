---
from: csharp
status: draft
concept: "Operating what you deployed"
from_way: "NuGet lock files, Serilog rolling files, Hangfire or Quartz.NET, and Windows Server administration."
native_way: "Linux operations: apt, logrotate, timers, backups and Ansible or cloud-init."
sensei_tip: "Hangfire and Quartz jobs run inside the app and need a shared store to avoid duplicates. For plain nightly tasks, a systemd timer calling a small console app is simpler and logs to the journal."
---

| C# | Linux |
| --- | --- |
| packages.lock.json | apt-mark hold |
| Serilog rolling file | logrotate, or console plus journal |
| Quartz.NET / Hangfire | cron or a systemd timer |
| Windows Server Backup | restic or borg, plus restore tests |
| Group Policy / DSC | Ansible, cloud-init |
