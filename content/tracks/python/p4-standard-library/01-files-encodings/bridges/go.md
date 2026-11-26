---
from: go
status: draft
concept: "Files, paths and text I/O"
from_way: "os.ReadFile, bufio.Scanner for lines, defer f.Close(), encoding/csv, encoding/json with struct tags, and os.Rename for atomic writes."
native_way: "pathlib, with blocks instead of defer, iterating a file, csv and json modules, os.replace, and exceptions instead of returned errors."
sensei_tip: "Go strings are bytes and you decode when you must; Python str is always decoded text, and the encoding is chosen when the file is opened."
---

| Go | Python 3.15 |
| --- | --- |
| `filepath.Join(dir, "a.csv")` | `dir / "a.csv"` |
| `os.ReadFile(p)` | `p.read_bytes()` or `p.read_text(encoding="utf-8")` |
| `bufio.NewScanner(f)` + `defer f.Close()` | `with open(p, encoding="utf-8") as f: for line in f:` |
| `csv.NewReader(f)` | `csv.reader(f)` / `csv.DictReader(f)` |
| `json.Marshal(v)` with struct tags | `json.dumps(v)`, or Pydantic models |
| `os.Rename(tmp, target)` | `os.replace(tmp, target)` |
| `if errors.Is(err, fs.ErrNotExist)` | `except FileNotFoundError:` |
