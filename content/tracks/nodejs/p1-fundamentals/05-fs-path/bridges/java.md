---
from: java
status: draft
concept: "Files and paths"
from_way: "java.nio.file: Path.of, Files.readString, Files.walk, Files.move with ATOMIC_MOVE, and exceptions such as NoSuchFileException."
native_way: "node:fs/promises and node:path: readFile, readdir with recursive or glob, rename for atomic replacement, and errors with a code such as ENOENT."
sensei_tip: "Node.js has no exception hierarchy for files: one Error type with a code property. Where Java catches NoSuchFileException, check error.code === \"ENOENT\"."
---

| Java NIO | Node.js |
| --- | --- |
| `Path.of("a", "b")` | `path.join("a", "b")` |
| `Files.readString(p)` | `await readFile(p, "utf8")` |
| `Files.walk(dir)` | `readdir(dir, { recursive: true })`, `glob` |
| `Files.move(tmp, p, ATOMIC_MOVE)` | `await rename(tmp, p)` |
| `NoSuchFileException` | `error.code === "ENOENT"` |
| `getClass().getResource("x")` | `path.join(import.meta.dirname, "x")` |
