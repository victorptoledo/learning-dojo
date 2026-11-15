---
from: csharp
status: draft
concept: "Remotes and remote-tracking branches"
from_way: "TFVC Get Latest, which updates your workspace straight from the server."
native_way: "Git splits Get Latest in two: fetch (download, nothing changes locally) and merge or rebase (integrate when you are ready)."
sensei_tip: "Visual Studio's Sync button runs pull and push together. Prefer Fetch, look at the incoming commits, then Pull: you choose when your files change."
---

| TFVC / Visual Studio | Git |
| --- | --- |
| Get Latest | `git fetch` + `git merge` (or `git pull`) |
| Check in (to the server) | `git commit` (local) + `git push` |
| Sync button | `git pull` then `git push` |
| Server path `$/Shop/Main` | `origin/main` |
