---
from: javascript
status: draft
concept: "Staging and atomic commits"
from_way: "Committing package.json changes, a lock file update and feature code together after a long session."
native_way: "Stage the dependency upgrade (package.json and lock file) as one commit and the feature as another, so each can be reviewed and reverted on its own."
sensei_tip: "lint-staged runs your linter only on the staged files, which is exactly the content of the next commit, not the half-finished files in the working tree."
---

| Node habit | With Git |
| --- | --- |
| `git add .` after `npm install` | stage `package.json` and the lock file together, deliberately |
| `console.log` left in | review `git diff --staged` |
| `dist/` in the commit | `.gitignore` |
| "Who added this dependency?" | `git log -S '"left-pad"' -- package.json` |
