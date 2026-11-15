---
from: javascript
status: draft
concept: "Branches as pointers"
from_way: "Feature flags in code for every experiment, or a second clone of the repository."
native_way: "A branch per change, switched in place; node_modules stays, only tracked files change."
sensei_tip: "When package-lock.json differs between branches, run npm ci after switching. node_modules is ignored, so Git does not switch it for you."
---

| Node habit | With Git |
| --- | --- |
| a second clone to try something | `git switch -c try-vite` |
| `if (process.env.NEW_UI)` for every idea | short-lived branches (flags for long rollouts) |
| node_modules out of date after a switch | `npm ci` |
| "Did my fix reach main?" | `git branch --merged main` |
