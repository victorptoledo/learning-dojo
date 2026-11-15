---
from: python
status: draft
concept: "Content-addressed, immutable objects"
from_way: "A dict from file names to mutable contents that you overwrite in place."
native_way: "A dict from hash(content) to content that only ever grows, plus a few names (refs) pointing at entries."
sensei_tip: "You can reproduce a blob hash in Python: hashlib.sha1(b'blob 6\\0hello\\n').hexdigest() gives the same value as echo hello | git hash-object --stdin."
---

| Python | Git |
| --- | --- |
| `store[sha1(data)] = data` | `git hash-object -w` |
| `store[key]` | `git cat-file -p HASH` |
| a frozen dataclass or a tuple | an object: it can never change |
| `main = commit_hash` (rebinding a name) | moving a branch |
