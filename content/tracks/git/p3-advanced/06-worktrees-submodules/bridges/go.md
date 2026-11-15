---
from: go
status: draft
concept: "Sharing code between repositories"
from_way: "Vendoring with go mod vendor, or a replace directive to a local folder."
native_way: "Go modules already pin dependencies by version and hash; a go.work workspace replaces submodules for local multi-module work."
sensei_tip: "go.work (go work use ./api ./shop) lets you edit two modules side by side without replace lines in go.mod. Do not commit go.work for libraries; it is a local convenience."
---

| Go habit | Git option |
| --- | --- |
| `go mod vendor` | already a copy, like a subtree |
| `replace ../lib` | `go.work`, no submodule needed |
| several modules in one repository | a monorepo with tags `api/v1.2.0` |
| review a branch while coding | `git worktree add ../review feature/x` |
