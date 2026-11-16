---
from: python
status: draft
concept: "Virtual memory and Python"
from_way: "Believing freed Python objects always give memory back to the OS."
native_way: "CPython's allocator keeps small-object arenas and may not return pages; mmap maps files into memory; fork shares pages copy-on-write, but reference counting writes to objects and copies their pages."
sensei_tip: "Use `mmap` to work on huge files without reading them, and `gc.freeze()` before forking workers to reduce copy-on-write page copies."
---

| In Python | Underneath |
| --- | --- |
| `mmap.mmap(f.fileno(), 0)` | a file as memory |
| `os.fork()` | copy-on-write pages |
| `resource.getrusage` | max RSS |
| `gc.freeze()` | keeps objects out of later collections, fewer page writes |
| `MemoryError` | an allocation the OS refused |
