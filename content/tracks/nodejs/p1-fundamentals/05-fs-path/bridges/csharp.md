---
from: csharp
status: draft
concept: "Files and paths"
from_way: "System.IO: Path.Combine, File.ReadAllTextAsync, Directory.EnumerateFiles, File.Move with overwrite, FileNotFoundException, and AppContext.BaseDirectory."
native_way: "node:fs/promises and node:path: path.join, readFile, readdir or glob, rename, errors with a code such as ENOENT, and import.meta.dirname."
sensei_tip: "await using a FileHandle is the counterpart of await using a FileStream: the handle is closed even when an exception is thrown."
---

| C# | Node.js |
| --- | --- |
| `Path.Combine("a", "b")` | `path.join("a", "b")` |
| `File.ReadAllTextAsync(p)` | `readFile(p, "utf8")` |
| `Directory.EnumerateFiles(d, "*.cs", AllDirectories)` | `glob("**/*.ts", { cwd: d })` |
| `File.Move(tmp, p, overwrite: true)` | `rename(tmp, p)` |
| `FileNotFoundException` | `code === "ENOENT"` |
| `AppContext.BaseDirectory` | `import.meta.dirname` |
