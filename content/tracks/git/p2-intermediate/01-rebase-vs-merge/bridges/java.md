---
from: java
status: draft
concept: "History hygiene"
from_way: "Merging `main` into your branch over and over, filling the log with 'Merge branch main' commits."
native_way: "`git rebase main` replays your commits on top of the updated `main`, producing a clean timeline."
sensei_tip: "You almost never lose work in a local repository: after a bad `git reset --hard`, run `git reflog` and recover the exact hash you were on."
---
