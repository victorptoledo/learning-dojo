---
from: python
status: draft
concept: "Files and paths"
from_way: "pathlib.Path with read_text, iterdir, rglob and replace, exceptions such as FileNotFoundError and PermissionError, and __file__ for the module's location."
native_way: "node:fs/promises with readFile, readdir, glob and rename, a single Error type with a code such as ENOENT or EACCES, and import.meta.dirname for the module's location."
sensei_tip: "Path(__file__).parent / 'x' becomes path.join(import.meta.dirname, 'x'). And since everything is async, a file read is a promise you await, not a call that returns."
---

| Python | Node.js |
| --- | --- |
| `Path("a") / "b"` | `path.join("a", "b")` |
| `p.read_text()` | `await readFile(p, "utf8")` |
| `p.rglob("*.ts")` | `glob("**/*.ts")` |
| `os.replace(tmp, p)` | `await rename(tmp, p)` |
| `FileNotFoundError` | `error.code === "ENOENT"` |
| `Path(__file__).parent` | `import.meta.dirname` |
