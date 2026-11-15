---
from: csharp
status: draft
concept: "Undoing changes"
from_way: "TFVC Undo Pending Changes and Rollback, which always create a new changeset on the server."
native_way: "Git restore for pending edits, reset for local commits that only you have, revert as the equivalent of a TFVC rollback."
sensei_tip: "TFVC's Rollback is Git's revert. Git's reset has no TFVC equivalent because TFVC history lives on the server and cannot be rewritten locally."
---

| TFVC | Git |
| --- | --- |
| Undo Pending Changes | `git restore FILE` |
| Exclude from check-in | `git restore --staged FILE` |
| Rollback changeset | `git revert <hash>` |
| (none) | `git reset` for local commits |
