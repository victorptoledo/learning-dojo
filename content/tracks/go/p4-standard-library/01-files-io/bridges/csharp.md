---
from: csharp
status: draft
concept: "Streams, File and Path in .NET"
from_way: "Stream, StreamReader and StreamWriter, File.ReadAllText and File.ReadLines, using statements, Stream.CopyTo, File.Move with overwrite or File.Replace, and Path.Combine."
native_way: "io.Reader and io.Writer, os.ReadFile and bufio.Scanner, defer Close, io.Copy, os.Rename over the old file, and filepath.Join; an fs.FS replaces IFileProvider for read-only trees."
sensei_tip: "Stream in .NET is one big abstract class with CanRead, CanWrite and CanSeek. Go splits that into small interfaces (io.Reader, io.Writer, io.Seeker, io.Closer) and you ask only for the ones you need."
---

| C# | Go |
| --- | --- |
| `Stream` (read side) | `io.Reader` |
| `Stream` (write side) | `io.Writer` |
| `File.ReadAllText(p)` | `os.ReadFile(p)` |
| `File.ReadLines(p)` | `bufio.NewScanner(f)` |
| `using var f = File.OpenRead(p);` | `f, err := os.Open(p)`; `defer f.Close()` |
| `src.CopyTo(dst)` | `io.Copy(dst, src)` |
| `File.Move(tmp, target, overwrite: true)` | `os.Rename(tmp, target)` |
| `Path.Combine("out", "a.json")` | `filepath.Join("out", "a.json")` |
| `IFileProvider` / embedded resources | `fs.FS` / `embed.FS` |
