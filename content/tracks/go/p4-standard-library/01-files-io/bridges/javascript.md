---
from: javascript
status: draft
concept: "fs, streams and path in Node.js"
from_way: "fs.promises.readFile, readable and writable streams piped together, readline for lines, path.join, and fs.rename for an atomic swap, mostly with async callbacks or await."
native_way: "Blocking calls in a goroutine: os.ReadFile, io.Copy between a Reader and a Writer, bufio.Scanner for lines, filepath.Join and os.Rename. The runtime parks the goroutine while the system call runs."
sensei_tip: "There are no Sync and async versions of each function in Go. Every file call blocks only its own goroutine, so plain sequential code is already the non-blocking version."
---

| JavaScript | Go |
| --- | --- |
| `await fs.promises.readFile(p)` | `os.ReadFile(p)` |
| `src.pipe(dst)` / `stream.pipeline` | `io.Copy(dst, src)` |
| `readline.createInterface({ input })` | `bufio.NewScanner(r)` |
| `fs.createWriteStream(p)` | `os.Create(p)` + `bufio.NewWriter` (then `Flush`) |
| `fs.rename(tmp, target)` | `os.Rename(tmp, target)` |
| `path.join("out", "a.json")` | `filepath.Join("out", "a.json")` |
| `err.code === "ENOENT"` | `errors.Is(err, fs.ErrNotExist)` |
| `memfs` in tests | `fstest.MapFS` |
