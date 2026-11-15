---
from: go
status: draft
concept: "Undoing changes"
from_way: "Re-running go mod tidy and hand-editing go.mod until the build works."
native_way: "Restore go.mod and go.sum from the last commit, and revert a released change instead of moving its tag."
sensei_tip: "If a bad version was tagged and published, do not reset and re-tag it: revert, tag a new patch version, and add a retract directive to go.mod for the bad one."
---

| Go situation | With Git |
| --- | --- |
| go.mod broken by an upgrade | `git restore go.mod go.sum` |
| generated files to throw away | `git clean -n`, then `git clean -f` |
| bad release v1.4.0 published | `git revert`, tag v1.4.1, `retract v1.4.0` in go.mod |
| last local commit missed a file | `git commit --amend --no-edit` |
