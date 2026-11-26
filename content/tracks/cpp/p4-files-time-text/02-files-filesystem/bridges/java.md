---
from: java
status: draft
concept: "Files and the file system"
from_way: "java.nio.file with Path, Files.readAllLines, Files.newBufferedReader in try-with-resources, and Files.move with ATOMIC_MOVE."
native_way: "std::filesystem::path and its free functions, file streams closed by their destructors, and fs::rename for atomic replacement."
sensei_tip: "try-with-resources is what every C++ stream does by default: the destructor is the close()."
---

| Java | C++ |
| --- | --- |
| `Path.of(dir, name)` | `std::filesystem::path(dir) / name` |
| `try (var r = Files.newBufferedReader(p))` | `std::ifstream in(p);` (closed by the destructor) |
| `Files.list(dir)` | `std::filesystem::directory_iterator(dir)` |
| `Files.move(tmp, p, ATOMIC_MOVE)` | `std::filesystem::rename(tmp, p)` |
| `IOException` | `std::filesystem::filesystem_error` or a `std::error_code` |
