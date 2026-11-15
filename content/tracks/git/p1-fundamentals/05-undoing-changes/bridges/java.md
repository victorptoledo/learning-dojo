---
from: java
status: draft
concept: "Undoing changes"
from_way: "IntelliJ's Rollback button and Local History, which only know about your own machine."
native_way: "restore for files, reset for your unpushed commits, revert for commits the team already has."
sensei_tip: "IntelliJ's Revert Commit runs git revert and its Undo Commit runs git reset --soft HEAD~1. Knowing which one it does tells you whether it is safe after a push."
---

| IntelliJ action | Git command |
| --- | --- |
| Rollback (file) | `git restore FILE` |
| Unstage | `git restore --staged FILE` |
| Undo Commit | `git reset --soft HEAD~1` |
| Revert Commit | `git revert <hash>` |
| Reset Current Branch to Here (Hard) | `git reset --hard <hash>` |
