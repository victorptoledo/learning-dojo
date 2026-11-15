---
from: csharp
status: draft
concept: "Paths, the program search path and documentation"
from_way: "Directory.GetCurrentDirectory(), Path.Combine with backslashes on Windows, and Environment.SpecialFolder for user folders."
native_way: "Forward slashes, one tree from /, case-sensitive names, configuration in /etc and data in /var."
sensei_tip: "Path.Combine and Path.DirectorySeparatorChar already do the right thing on Linux; hard-coded \\ and C:\\ do not. AppContext.BaseDirectory points next to the binary, whatever the working directory."
---

| C# | Linux |
| --- | --- |
| C:\Program Files\App | /usr/bin, /usr/local/bin or /opt/app |
| %APPDATA% | ~/.config |
| Environment.CurrentDirectory | pwd |
| where dotnet | type dotnet |
| docs.microsoft.com | man and --help |
