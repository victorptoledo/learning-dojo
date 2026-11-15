---
from: java
status: draft
concept: "Remotes and remote-tracking branches"
from_way: "Maven's local repository (~/.m2) as a cache of what the remote repository had when you last built."
native_way: "origin/* are the same idea for commits: a local cache of the server's branches, refreshed by git fetch."
sensei_tip: "Just like mvn -o builds from ~/.m2 without the network, git log origin/main works offline from your last fetch. Fetch before you trust it."
---

| Maven | Git |
| --- | --- |
| remote repository (Central, Nexus) | remote (`origin`) |
| `~/.m2/repository` cache | `refs/remotes/origin/*` |
| `mvn -U` (update snapshots) | `git fetch` |
| `mvn deploy` | `git push` |
