---
from: java
status: draft
concept: "Resolving conflicts"
from_way: "IntelliJ's merge dialog: left, result and right panes, with Accept Left or Accept Right buttons."
native_way: "The same three-way merge: left is ours, right is theirs, the middle starts from the merge base. Accept-left or accept-right only when one side really wins."
sensei_tip: "Set IntelliJ (or any IDE) as git mergetool so command-line conflicts open in the three-pane view: git config --global merge.tool intellij plus its mergetool.intellij.cmd line."
---

| IntelliJ merge dialog | Git |
| --- | --- |
| Left pane | ours (HEAD) |
| Right pane | theirs |
| Result pane | the file you `git add` |
| "Apply non-conflicting changes" | what Git already merged on its own |
| Conflicting `pom.xml` versions | resolve, then `mvn -q verify` before continuing |
