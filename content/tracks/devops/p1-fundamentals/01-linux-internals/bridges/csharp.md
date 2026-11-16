---
from: csharp
status: draft
concept: "Diagnose a service from the host"
from_way: "On Windows you open Task Manager, Event Viewer and the Services console."
native_way: "On Linux, top replaces Task Manager, journalctl replaces Event Viewer and systemctl replaces the Services console."
sensei_tip: "dotnet-counters and dotnet-dump work on Linux too; add UseSystemd() to a worker service so systemd knows when it is ready."
---

| C# world | Linux |
| --- | --- |
| Services console | `systemctl status`, `systemctl restart` |
| Event Viewer | `journalctl` |
| Task Manager | `top`, `htop`, `ps` |
| `netstat -ano` | `ss -tlnp` |
