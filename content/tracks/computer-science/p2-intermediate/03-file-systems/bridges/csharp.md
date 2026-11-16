---
from: csharp
status: draft
concept: "Files and durability in C#"
from_way: "Thinking Flush() writes to disk."
native_way: "Stream.Flush() empties .NET buffers; FileStream.Flush(true) also flushes the OS buffers to the device; File.Replace and File.Move(overwrite) map to rename-style operations."
sensei_tip: "Use `FileStream.Flush(flushToDisk: true)` before a `File.Move(tmp, path, overwrite: true)` when a save must survive power loss."
---

| In C# | Underneath |
| --- | --- |
| `stream.Flush()` | .NET buffer to the OS |
| `fileStream.Flush(true)` | fsync / FlushFileBuffers |
| `File.Move(a, b, true)` | rename over |
| `FileOptions.WriteThrough` | write-through to the device |
| `FileSystemWatcher` | change notifications |
