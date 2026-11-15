---
from: java
status: draft
concept: "Branches as pointers"
from_way: "Copying the project folder to try an idea, or a long-lived SVN branch that is a full copy on the server."
native_way: "A branch is a 41-byte file; create one per idea and throw it away when it fails."
sensei_tip: "After switching branches, let Maven or Gradle rebuild: the class files in target/ belong to the branch you left. A quick mvn -q compile avoids chasing bugs that are not in your sources."
---

| Java habit | With Git |
| --- | --- |
| `cp -r shop shop-experiment` | `git switch -c experiment` |
| SVN `branches/release-2.4` | `release/2.4`, a pointer |
| Shelving in IntelliJ | `git stash` (IntelliJ's shelf is IDE-only) |
| Merge in the IDE | `git merge`; the IDE shows the same three-way view |
