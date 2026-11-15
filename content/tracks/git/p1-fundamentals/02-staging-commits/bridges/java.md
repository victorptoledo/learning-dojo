---
from: java
status: draft
concept: "Staging and atomic commits"
from_way: "IntelliJ's commit dialog with every changed file ticked, or changelists that group files."
native_way: "The index: you stage exactly the lines of the next commit, so a refactoring and a fix in the same class become two commits."
sensei_tip: "IntelliJ's commit dialog can stage single lines too (the checkboxes in the diff gutter). Use it, or git add -p, so a review sees the rename separately from the behaviour change."
---

| Java habit | With Git |
| --- | --- |
| Commit all changed files at once | `git add -p`, one logical change per commit |
| Rename a class with the IDE | its own commit; Git detects the rename by content |
| `target/`, `*.class` show up | `.gitignore` |
| "Who wrote this method?" | `git blame`, or `git log -L :methodName:File.java` |
