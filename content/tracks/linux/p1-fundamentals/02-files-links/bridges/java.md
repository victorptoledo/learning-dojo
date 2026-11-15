---
from: java
status: draft
concept: "Files, names and links"
from_way: "Files.copy, Files.move and Files.delete in java.nio, Explorer shortcuts (.lnk) that only Explorer understands, and a recycle bin."
native_way: "cp, mv and rm in the shell, symbolic links that every program follows, hard links as extra names for the same data, and no recycle bin."
sensei_tip: "Files.move with ATOMIC_MOVE is the same rename(2) system call that mv uses: atomic only on the same filesystem. That is why you write a temporary file next to the target and rename it."
---

| Java (java.nio.file) | Shell |
| --- | --- |
| `Files.copy(a, b)` | `cp a b` |
| `Files.move(a, b, ATOMIC_MOVE)` | `mv a b` (same filesystem) |
| `Files.delete(p)` | `rm p` |
| `Files.createSymbolicLink(link, target)` | `ln -s target link` |
| `Files.walk(dir).filter(...)` | `find dir -type f -name ...` |
| `Files.readAllLines(p)` | `cat p`, or `less p` for big files |
