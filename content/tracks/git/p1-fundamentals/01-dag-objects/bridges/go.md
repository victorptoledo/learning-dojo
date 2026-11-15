---
from: go
status: draft
concept: "Content-addressed, immutable objects"
from_way: "Structs you modify through pointers."
native_way: "Values identified by their hash, like the module cache and go.sum, where a module version is pinned by the hash of its content."
sensei_tip: "go.sum already taught you content addressing: if the bytes behind v1.2.0 change, the hash no longer matches and the build stops. Git applies the same rule to every file and commit."
---

| Go | Git |
| --- | --- |
| `sha1.Sum(data)` | the object key (with a small header) |
| go.sum line `h1:...` | a hash that pins content |
| a struct value, copied, never shared mutably | an immutable object |
| `main := &commit` | a branch pointing to a commit |
