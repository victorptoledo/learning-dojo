---
from: java
status: draft
concept: "Files, paths and text I/O"
from_way: "java.io.File, FileReader with the platform charset, and streams closed in finally blocks."
native_way: "Path and Files from NIO.2, UTF-8 by default, lazy line streams in try-with-resources and atomic moves."
sensei_tip: "If you still write new File(...) or new FileReader(...), switch to Path.of and Files: shorter code, better errors (NoSuchFileException) and an explicit charset."
---

| Old Java | Java 25 |
| --- | --- |
| `new File(dir, "a.csv")` | `dir.resolve("a.csv")` |
| `new BufferedReader(new FileReader(f))` | `Files.newBufferedReader(path)` |
| `file.listFiles()` | `Files.list(dir) in try-with-resources` |
| `FileWriter, then rename` | `Files.move(temp, target, ATOMIC_MOVE)` |
