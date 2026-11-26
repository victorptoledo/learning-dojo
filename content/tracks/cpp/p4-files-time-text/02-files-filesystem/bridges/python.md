---
from: python
status: draft
concept: "Files and the file system"
from_way: "pathlib.Path with / to join, with open(...) as f for closing, os.scandir, and os.replace for atomic writes."
native_way: "std::filesystem::path also joins with /, streams close in their destructors, and fs::rename is os.replace."
sensei_tip: "pathlib's API maps almost name for name: stem, suffix (extension), parent (parent_path), iterdir (directory_iterator)."
---

| Python | C++ |
| --- | --- |
| `Path(dir) / name` | `std::filesystem::path(dir) / name` |
| `with open(p) as f: for line in f:` | `std::ifstream in(p); while (std::getline(in, line))` |
| `p.stem`, `p.suffix`, `p.parent` | `p.stem()`, `p.extension()`, `p.parent_path()` |
| `os.replace(tmp, p)` | `std::filesystem::rename(tmp, p)` |
| `FileNotFoundError` | `std::errc::no_such_file_or_directory` in the error_code |
