---
from: csharp
status: draft
concept: "Staging and atomic commits"
from_way: "TFVC pending changes: every checked-out file goes into the check-in unless you exclude it."
native_way: "The index holds exactly what the next commit records, down to single lines, and you build it deliberately."
sensei_tip: "Visual Studio's Git Changes window has a Stage button per file and per line range. Staging is the habit that matters, whichever tool you use."
---

| TFVC / Visual Studio | Git |
| --- | --- |
| Pending changes | working tree changes (`git status`) |
| Included changes | the index (`git diff --staged`) |
| Excluded changes | unstaged changes |
| Check-in comment | commit message: subject, blank line, body |
| Annotate | `git blame` |
