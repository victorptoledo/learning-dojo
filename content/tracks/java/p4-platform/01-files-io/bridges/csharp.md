---
from: csharp
status: draft
concept: "Files, paths and text I/O"
from_way: "System.IO.Path, File.ReadAllText and using blocks."
native_way: "Path and Files with the same ideas, try-with-resources instead of using."
sensei_tip: "File.ReadLines in .NET is lazy like Files.lines; both hold the file open until the enumeration or stream is closed."
---

| C# | Java 25 |
| --- | --- |
| `Path.Combine(dir, "a.csv")` | `dir.resolve("a.csv")` |
| `File.ReadAllText(p)` | `Files.readString(p)` |
| `File.ReadLines(p)` | `Files.lines(p) in try-with-resources` |
| `File.Move(tmp, target, true)` | `Files.move(tmp, target, REPLACE_EXISTING, ATOMIC_MOVE)` |
