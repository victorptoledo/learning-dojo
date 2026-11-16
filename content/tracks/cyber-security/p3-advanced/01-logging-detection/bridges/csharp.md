---
from: csharp
status: draft
concept: "Logs that a detection can trust"
from_way: "_logger.LogInformation($\"Login {user}\") with string interpolation."
native_way: "Message templates with named properties (Serilog or Microsoft.Extensions.Logging) sent to a central sink, with sensitive properties destructured away."
sensei_tip: "Use templates, `_logger.LogWarning(\"Login failed for {UserId} from {Ip}\", id, ip)`, not interpolation: the values become structured properties a SIEM can query, and analyzers flag the interpolated form."
---

| C# | Security logging |
| --- | --- |
| $"Login {user}" | "Login failed for {UserId}" template |
| logging whole DTOs | explicit properties, no secrets |
| file sink on the VM | a central sink (Seq, Elastic, Sentinel) |
| no correlation | Activity and trace ids |
| exceptions with connection strings | scrubbed exception messages |
