---
from: csharp
status: draft
concept: "Getting and managing a development machine"
from_way: "NuGet packages per project, the .NET SDK from Microsoft's installer, and Windows with Visual Studio as the usual home."
native_way: ".NET runs natively on Linux: install the SDK from Microsoft's apt repository or use the mcr.microsoft.com/dotnet images; system tools come from apt."
sensei_tip: "ASP.NET Core on Linux is the normal production setup today. Develop on Windows, but run the tests in a Linux container early: file paths, case sensitivity and line ends differ."
---

| C# | Linux |
| --- | --- |
| dotnet add package | unchanged; system packages come from apt |
| Visual Studio installer | sudo apt install dotnet-sdk-8.0 from Microsoft's repository |
| IIS | Kestrel behind nginx, run by systemd |
| Windows Services | systemd units |
