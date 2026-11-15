---
from: csharp
status: draft
concept: "Who may do what with a file"
from_way: "Windows ACLs and the Properties > Security dialog, and running Visual Studio or services as an administrator."
native_way: "Three permission triplets per file plus optional ACLs, and sudo for single commands."
sensei_tip: "File.SetUnixFileMode(path, UnixFileMode.UserRead | UnixFileMode.UserWrite) (.NET 7+) sets modes from C#. ASP.NET Core does not need root: run it as its own user behind nginx."
---

| C# | Linux |
| --- | --- |
| Properties > Security | ls -l, chmod, chown, getfacl |
| Run as administrator | sudo command |
| File.SetUnixFileMode(p, ...) | chmod p |
| Administrators group | the sudo group |
| LocalSystem account | a dedicated service user |
