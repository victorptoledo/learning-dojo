---
from: javascript
status: draft
concept: "Polishing a branch before review"
from_way: "Committing every save, then relying on GitHub's squash merge to hide it."
native_way: "Keep the commits that tell the story, fold the rest with fixup and autosquash, and push the result with --force-with-lease."
sensei_tip: "With commitlint and Conventional Commits, each commit's subject may end up in the changelog. Interactive rebase lets you reword them (reword) before they become release notes."
---

| Node workflow | Git |
| --- | --- |
| "Squash and merge" | `git rebase -i` to keep 2-3 good commits instead |
| commitlint rejects a subject | `reword` it in the todo list |
| review fix | `git commit --fixup=<hash>` |
| `npm test` on each commit | `git rebase -i --exec "npm test" main` |
