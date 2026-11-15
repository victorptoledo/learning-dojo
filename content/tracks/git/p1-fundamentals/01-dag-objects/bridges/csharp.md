---
from: csharp
status: draft
concept: "Content-addressed, immutable objects"
from_way: "TFVC changesets numbered by the server, each storing the changes to individual files."
native_way: "Commits identified by the hash of their content, each a full snapshot, created on your machine without asking a server for a number."
sensei_tip: "A C# record compares by value; a Git object is addressed by value. Two people creating the same blob on different machines get the same hash."
---

| C# / TFVC | Git |
| --- | --- |
| changeset 1234, assigned by the server | commit hash, computed locally |
| `record Commit(...)` with value equality | object identity is the content hash |
| `ImmutableList<T>` | parents and trees never change |
| a reference variable | a branch ref |
