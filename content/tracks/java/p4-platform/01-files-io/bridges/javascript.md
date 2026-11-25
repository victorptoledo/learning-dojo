---
from: javascript
status: draft
concept: "Files, paths and text I/O"
from_way: "fs.promises, path.join and streams in Node.js."
native_way: "Blocking Files calls (cheap on virtual threads), Path.resolve and lazy line streams."
sensei_tip: "Java file calls block, which is fine: run many of them on virtual threads instead of chaining promises."
---

| Node.js | Java 25 |
| --- | --- |
| `path.join(dir, "a.csv")` | `dir.resolve("a.csv")` |
| `await fs.readFile(p, "utf8")` | `Files.readString(p)` |
| `readline over createReadStream` | `Files.lines(p) in try-with-resources` |
| `fs.rename(tmp, target)` | `Files.move(tmp, target, ATOMIC_MOVE)` |
