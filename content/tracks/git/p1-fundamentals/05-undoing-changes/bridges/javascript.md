---
from: javascript
status: draft
concept: "Undoing changes"
from_way: "Deleting node_modules and the lock file and reinstalling until it works again."
native_way: "Restore the committed lock file, then reinstall: git restore package-lock.json && npm ci."
sensei_tip: "git clean -fdx also deletes node_modules and .env because they are ignored. Run git clean -ndx first and read the list."
---

| Node habit | With Git |
| --- | --- |
| `rm package-lock.json && npm i` | `git restore package-lock.json && npm ci` |
| undo a bad `npm audit fix` | `git restore package.json package-lock.json` |
| remove build output | `git clean -ndX` (capital X: only ignored files) |
| roll back a released change | `git revert`, then release again |
