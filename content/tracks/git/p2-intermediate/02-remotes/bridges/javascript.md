---
from: javascript
status: draft
concept: "Remotes and remote-tracking branches"
from_way: "npm install github:org/lib#main, resolved to whatever the branch is at install time."
native_way: "A remote branch is a moving name; the lock file records the commit it resolved to, exactly like origin/main versus a hash."
sensei_tip: "npm writes the resolved commit of a Git dependency into package-lock.json. That is why npm ci is reproducible even when the branch moves."
---

| npm | Git |
| --- | --- |
| `github:org/lib#main` | the branch `main` on that remote |
| resolved commit in the lock file | a hash |
| `npm view lib versions` | `git ls-remote --tags origin` |
| `npm publish` | `git push` (for code) |
