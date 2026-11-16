---
from: go
status: draft
concept: "Files and paths"
from_way: "os.ReadFile, os.ReadDir with DirEntry, filepath.Join and filepath.WalkDir, os.Rename for atomic replacement, and errors.Is(err, fs.ErrNotExist)."
native_way: "readFile, readdir with withFileTypes (Dirent), path.join and readdir recursive or glob, rename, and error.code === \"ENOENT\"."
sensei_tip: "Dirent is Go's DirEntry: the type without an extra stat. errors.Is(err, fs.ErrNotExist) becomes a check of error.code, and like in Go you handle the error instead of checking os.Stat first."
---

| Go | Node.js |
| --- | --- |
| `os.ReadFile(p)` | `await readFile(p)` |
| `os.ReadDir(d)` | `readdir(d, { withFileTypes: true })` |
| `filepath.Join` | `path.join` |
| `filepath.WalkDir` | `readdir(d, { recursive: true })` |
| `errors.Is(err, fs.ErrNotExist)` | `error.code === "ENOENT"` |
| `os.Rename(tmp, p)` | `rename(tmp, p)` |
