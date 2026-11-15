---
from: csharp
status: draft
concept: "Service addresses and ports"
from_way: "launchSettings.json sets applicationUrl to http://localhost:5000 and the connection string uses Server=localhost."
native_way: "In a container, Kestrel must listen on all interfaces (ASPNETCORE_HTTP_PORTS=8080 or ASPNETCORE_URLS=http://+:8080) and connection strings use the service name."
sensei_tip: "launchSettings.json is not used by dotnet in a container; configure URLs with environment variables."
---

| C# / .NET | Container networking |
| --- | --- |
| `http://localhost:5000` in launchSettings | `ASPNETCORE_HTTP_PORTS=8080` |
| `Server=localhost;Database=app` | `Server=db;Database=app` |
| Windows Firewall rules | published ports and network isolation |
| `Dns.GetHostName()` | the container ID or `--hostname` |
