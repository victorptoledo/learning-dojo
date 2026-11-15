---
from: csharp
status: draft
concept: "Persistent state outside the process"
from_way: "An IIS site writes to App_Data and a local SQL Server or SQLite file survives on the server's disk."
native_way: "Containers are recreated on every deployment: mount a named volume for files the app must keep, and keep databases in their own containers with their own volumes."
sensei_tip: "ASP.NET Core Data Protection keys default to a folder in the container: persist them on a volume (or a key store), or every restart logs users out."
---

| C# / .NET | Container storage |
| --- | --- |
| `App_Data` or a local folder | a named volume |
| `appsettings.Production.json` on the server | a read-only bind mount or environment variables |
| Data Protection key ring | a volume or an external key store |
| `Path.GetTempPath()` | a tmpfs mount on `/tmp` |
