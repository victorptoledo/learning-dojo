---
from: go
status: draft
concept: "Files and the file system"
from_way: "os and path/filepath with filepath.Join, os.ReadDir, bufio.Scanner, defer f.Close(), and os.Rename for atomic writes."
native_way: "std::filesystem::path with /, directory_iterator, std::getline on a stream, destructors instead of defer, and fs::rename."
sensei_tip: "Go's `if err != nil` after each call is the std::error_code overload style; the exception overloads are the other option."
---

| Go | C++ |
| --- | --- |
| `filepath.Join(dir, name)` | `std::filesystem::path(dir) / name` |
| `f, err := os.Open(p); defer f.Close()` | `std::ifstream in(p); if (!in) { ... }` |
| `bufio.NewScanner(f)` + `Scan()` | `while (std::getline(in, line))` |
| `os.ReadDir(dir)` | `std::filesystem::directory_iterator(dir)` |
| `os.Rename(tmp, p)` | `std::filesystem::rename(tmp, p)` |
