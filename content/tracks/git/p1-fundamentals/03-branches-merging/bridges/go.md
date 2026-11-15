---
from: go
status: draft
concept: "Branches as pointers"
from_way: "A replace directive in go.mod pointing at a local copy to try a change."
native_way: "A branch in the dependency's repository; `go get example.com/lib@my-branch` resolves it to a pseudo-version pinned to the branch's tip commit."
sensei_tip: "go get module@branch records the commit, not the branch name: the pseudo-version (v0.0.0-20261114-abcdef123456) contains the tip's hash, so a later push to the branch does not change your build."
---

| Go | Git |
| --- | --- |
| `replace example.com/lib => ../lib` | a branch in `../lib` |
| `go get example.com/lib@feature` | the tip commit of `feature` |
| pseudo-version `v0.0.0-...-abcdef123456` | a commit hash |
| `go build` after switching | rebuild; the build cache is keyed by content anyway |
