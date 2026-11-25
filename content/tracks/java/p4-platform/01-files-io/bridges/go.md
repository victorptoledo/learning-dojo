---
from: go
status: draft
concept: "Files, paths and text I/O"
from_way: "os.ReadFile, filepath.Join, bufio.Scanner and defer f.Close()."
native_way: "Files and Path, try-with-resources instead of defer."
sensei_tip: "try-with-resources is defer Close tied to a block; errors arrive as IOException subtypes instead of error values."
---

| Go | Java 25 |
| --- | --- |
| `filepath.Join(dir, "a.csv")` | `dir.resolve("a.csv")` |
| `os.ReadFile(p)` | `Files.readAllBytes(p) or Files.readString(p)` |
| `bufio.NewScanner(f)` | `Files.newBufferedReader(p) or Files.lines(p)` |
| `os.Rename(tmp, target)` | `Files.move(tmp, target, ATOMIC_MOVE)` |
