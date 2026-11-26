---
from: java
status: draft
concept: "java.io, java.nio.file and try-with-resources"
from_way: "InputStream and OutputStream (or Reader and Writer for text), Files.readAllBytes and Files.lines, BufferedReader.readLine, try-with-resources closing everything, Files.move with ATOMIC_MOVE, and Path.resolve."
native_way: "io.Reader and io.Writer for every stream, os.ReadFile and bufio.Scanner, defer Close (checked for files you write), os.Rename for an atomic replace, filepath.Join, and fs.FS for a whole tree."
sensei_tip: "There is no try-with-resources: defer f.Close() right after the error check does the same job. For files you write, turn the deferred Close into a checked one, since try-with-resources would have thrown that exception for you."
---

| Java | Go |
| --- | --- |
| `InputStream` / `OutputStream` | `io.Reader` / `io.Writer` |
| `Files.readAllBytes(path)` | `os.ReadFile(name)` |
| `Files.lines(path)` / `BufferedReader.readLine()` | `bufio.NewScanner(f)` and `sc.Scan()` |
| `try (var in = Files.newInputStream(p)) { ... }` | `f, err := os.Open(name)`; `defer f.Close()` |
| `in.transferTo(out)` | `io.Copy(w, r)` |
| `Files.move(tmp, target, ATOMIC_MOVE)` | `os.Rename(tmp, target)` |
| `Path.of("out").resolve("a.json")` | `filepath.Join("out", "a.json")` |
| `FileSystems.newFileSystem(zip)` | `zip.NewReader`, an `fs.FS` |
| `NoSuchFileException` | `errors.Is(err, fs.ErrNotExist)` |
