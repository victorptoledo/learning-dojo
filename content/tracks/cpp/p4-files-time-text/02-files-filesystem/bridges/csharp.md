---
from: csharp
status: draft
concept: "Files and the file system"
from_way: "System.IO with Path.Combine, File.ReadLines, using statements for streams, Directory.EnumerateFiles and File.Replace or File.Move."
native_way: "std::filesystem::path with /, file streams that close in their destructors, directory_iterator, and fs::rename."
sensei_tip: "A C++ stream behaves as if it were always inside a using block."
---

| C# | C++ |
| --- | --- |
| `Path.Combine(dir, name)` | `std::filesystem::path(dir) / name` |
| `foreach (var line in File.ReadLines(p))` | `while (std::getline(in, line))` |
| `Directory.EnumerateFiles(dir, "*.log")` | `directory_iterator` + `extension() == ".log"` |
| `File.Move(tmp, p, overwrite: true)` | `std::filesystem::rename(tmp, p)` |
| `IOException` | `filesystem_error`, or the `std::error_code` overloads |
