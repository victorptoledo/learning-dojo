---
from: csharp
status: draft
concept: "Connecting to other services"
from_way: "HttpRequestException and SocketException with error codes, ipconfig and Test-NetConnection on Windows."
native_way: "ip, ss, getent, dig, nc and curl, one per layer."
sensei_tip: "Test-NetConnection host -Port 443 is the PowerShell version of nc -zv. Kestrel listens on localhost by default; set ASPNETCORE_URLS=http://0.0.0.0:8080 in containers."
---

| C# | Linux |
| --- | --- |
| ipconfig /all | ip -br addr, ip route |
| netstat -ano | ss -tlnp |
| Test-NetConnection -Port | nc -zv host port |
| Dns.GetHostAddresses(h) | getent hosts h |
| PuTTY | ssh and ~/.ssh/config |
