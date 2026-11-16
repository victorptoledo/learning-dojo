---
from: python
status: draft
concept: "Files and durability in Python"
from_way: "Assuming the with block guarantees data on disk."
native_way: "Closing a file flushes Python's buffer to the OS; os.fsync(f.fileno()) forces it to the device; os.replace is an atomic rename."
sensei_tip: "Safe save: write to a temporary file in the same directory, `f.flush(); os.fsync(f.fileno())`, `os.replace(tmp, path)`, then fsync the directory descriptor."
---

| In Python | Underneath |
| --- | --- |
| `f.flush()` | Python buffer to the page cache |
| `os.fsync(fd)` | page cache to the device |
| `os.replace(a, b)` | atomic rename |
| `os.stat(p).st_ino` | the inode number |
| `os.link` / `os.symlink` | hard and symbolic links |
