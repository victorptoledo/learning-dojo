---
from: csharp
status: draft
concept: "Running an application as a service"
from_way: "Windows Services through UseWindowsService, or IIS hosting ASP.NET Core."
native_way: "A systemd unit, with Microsoft.Extensions.Hosting.Systemd to tell systemd when the app is ready."
sensei_tip: "Call UseSystemd() on the host builder and set Type=notify: systemd then knows when the app is really started, and logs get proper priority levels in the journal."
---

| C# | Linux |
| --- | --- |
| UseWindowsService() | UseSystemd() and a unit file |
| Services console | systemctl status |
| Event Viewer | journalctl -u app |
| Task Scheduler | a .timer unit |
| Recovery options | Restart=on-failure |
