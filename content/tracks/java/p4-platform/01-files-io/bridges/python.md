---
from: python
status: draft
concept: "Files, paths and text I/O"
from_way: "pathlib.Path, open() with an encoding, and with blocks."
native_way: "Path and Files, try-with-resources, Files.lines for lazy reading."
sensei_tip: "try-with-resources is the with block of Python; Files.lines is like iterating over an open file, and must be closed the same way."
---

| Python | Java 25 |
| --- | --- |
| `Path("imports") / "a.csv"` | `Path.of("imports").resolve("a.csv")` |
| `path.read_text(encoding="utf-8")` | `Files.readString(path)` |
| `for line in open(path):` | `try (var lines = Files.lines(path)) { ... }` |
| `os.replace(tmp, target)` | `Files.move(tmp, target, ATOMIC_MOVE)` |
