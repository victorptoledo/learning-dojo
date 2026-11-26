---
from: java
status: draft
concept: "Files, paths and text I/O"
from_way: "Path and Files (NIO.2), try-with-resources, Files.lines for lazy reading, a CSV library, Jackson for JSON, and Files.move with ATOMIC_MOVE."
native_way: "pathlib.Path, with blocks, iterating a file object, the csv and json modules in the standard library, and os.replace."
sensei_tip: "A with block is try-with-resources; iterating an open file is Files.lines without the stream API, and it must be closed the same way."
---

| Java 25 | Python 3.15 |
| --- | --- |
| `Path.of("imports").resolve("a.csv")` | `Path("imports") / "a.csv"` |
| `Files.readString(path)` | `path.read_text(encoding="utf-8")` |
| `try (var lines = Files.lines(path)) { ... }` | `with path.open(encoding="utf-8") as f: for line in f: ...` |
| `Files.move(tmp, target, ATOMIC_MOVE)` | `os.replace(tmp, target)` |
| Jackson `ObjectMapper` | `json.loads`, `json.dumps` (or Pydantic for typed data) |
| Commons CSV / OpenCSV | `csv.DictReader`, `csv.DictWriter` |
| `@TempDir Path dir` | `tmp_path` fixture |
