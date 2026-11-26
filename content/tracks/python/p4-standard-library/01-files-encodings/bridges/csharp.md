---
from: csharp
status: draft
concept: "Files, paths and text I/O"
from_way: "System.IO with Path.Combine, File.ReadAllText, StreamReader in a using block, a CSV library such as CsvHelper, System.Text.Json, and File.Move with overwrite."
native_way: "pathlib.Path, with blocks, iterating a file object, csv and json in the standard library, and os.replace."
sensei_tip: "A using block is a with block. File.ReadLines (lazy) is the Python file iterator; File.ReadAllLines is readlines()."
---

| C# | Python 3.15 |
| --- | --- |
| `Path.Combine(dir, "a.csv")` | `dir / "a.csv"` |
| `File.ReadAllText(p, Encoding.UTF8)` | `p.read_text(encoding="utf-8")` |
| `foreach (var line in File.ReadLines(p))` | `for line in p.open(encoding="utf-8"):` |
| `new UTF8Encoding(true)` (BOM) | `encoding="utf-8-sig"` |
| `JsonSerializer.Serialize(x)` | `json.dumps(x)` |
| `File.Move(tmp, target, overwrite: true)` | `os.replace(tmp, target)` |
| `IOException`, `FileNotFoundException` | `OSError`, `FileNotFoundError` |
