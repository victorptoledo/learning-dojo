---
from: csharp
status: draft
concept: "Files, names and links"
from_way: "File.Copy, File.Move, File.Delete and Directory APIs, with Windows shortcuts (.lnk) that only Explorer understands."
native_way: "cp, mv and rm, and real symbolic links that every program follows."
sensei_tip: "File.Move(tmp, path, overwrite: true) is a rename on Linux too: atomic on the same filesystem. File.CreateSymbolicLink exists since .NET 6 and works without admin rights on Linux."
---

| C# | Linux |
| --- | --- |
| File.Copy(a, b) | cp a b |
| File.Move(a, b, true) | mv a b |
| Directory.Delete(p, true) | rm -r p |
| File.CreateSymbolicLink(link, target) | ln -s target link |
| Directory.EnumerateFiles(dir, "*.log", AllDirectories) | find dir -name '*.log' |
