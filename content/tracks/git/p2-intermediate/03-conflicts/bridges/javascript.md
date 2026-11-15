---
from: javascript
status: draft
concept: "Resolving conflicts"
from_way: "VS Code's 'Accept Current Change' and 'Accept Incoming Change' buttons above each conflict."
native_way: "Current is ours, Incoming is theirs; VS Code's merge editor also shows the base, like zdiff3."
sensei_tip: "During a rebase VS Code's 'Current' is main and 'Incoming' is your own commit. Read the labels, not the colours."
---

| VS Code | Git |
| --- | --- |
| Accept Current Change | `--ours` for that hunk |
| Accept Incoming Change | `--theirs` for that hunk |
| Accept Both Changes | both versions, one after the other (often wrong for code) |
| Merge editor's base view | `merge.conflictStyle zdiff3` |
| lock-file conflict | regenerate with `npm install` |
