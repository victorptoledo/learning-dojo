---
from: python
status: draft
concept: "open(), with and pathlib"
from_way: "with open(path) as f for files, iterating over f line by line, pathlib.Path for paths, shutil.copyfileobj between streams, os.replace for an atomic swap, and io.StringIO in tests."
native_way: "os.Open with defer Close, bufio.Scanner for lines, filepath for paths, io.Copy between any reader and writer, os.Rename for the swap, and strings.NewReader or fstest.MapFS in tests."
sensei_tip: "Iterating over a Python file object hides errors in exceptions. In Go, the Scan loop just stops, so the sc.Err() check after the loop is your except clause. Forgetting it turns a read failure into a silently short file."
---

| Python | Go |
| --- | --- |
| `with open(p) as f:` | `f, err := os.Open(p)`; `defer f.Close()` |
| `for line in f:` | `for sc.Scan() { line := sc.Text() }` and then `sc.Err()` |
| `Path(p).read_text()` | `os.ReadFile(p)` |
| `shutil.copyfileobj(src, dst)` | `io.Copy(dst, src)` |
| `os.replace(tmp, target)` | `os.Rename(tmp, target)` |
| `tempfile.NamedTemporaryFile(dir=d, delete=False)` | `os.CreateTemp(d, ".x-*")` |
| `pathlib.Path("out") / "a.json"` | `filepath.Join("out", "a.json")` |
| `os.walk(root)` | `filepath.WalkDir(root, fn)` |
| `io.StringIO("...")` | `strings.NewReader("...")` |
