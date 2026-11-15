---
from: csharp
status: draft
concept: "Branches as pointers"
from_way: "TFVC branches: full server-side copies of a folder, created rarely and merged with ceremony."
native_way: "Git branches: pointers created in milliseconds, one per task, merged and deleted within days."
sensei_tip: "Close Visual Studio or let it reload when you switch branches that add or remove projects: the .sln and .csproj files change under it."
---

| TFVC | Git |
| --- | --- |
| Branch = copy of `$/Shop/Main` | branch = pointer to a commit |
| Merge with a baseless merge wizard | `git merge`, always with a merge base |
| Shelveset | `git stash` or a WIP branch |
| Get specific version | `git switch --detach <hash>` |
