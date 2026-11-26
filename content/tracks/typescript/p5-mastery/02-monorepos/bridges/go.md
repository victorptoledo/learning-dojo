---
from: go
status: draft
concept: "Modules and workspaces"
from_way: "A go.mod per module, go.work to develop several modules together, packages with internal/ for boundaries, and a build cache that makes rebuilds incremental."
native_way: "Workspaces link packages, exports defines boundaries, and project references plus .tsbuildinfo give TypeScript an incremental, ordered build."
sensei_tip: "go.work and npm workspaces are siblings. Go's internal/ rule is enforced by the compiler; in TypeScript, exports and references are how you get the same guarantee."
---

| Go | TypeScript |
| --- | --- |
| `go.work` with `use ./domain ./api` | `"workspaces": ["domain", "api"]` |
| `internal/` packages | Paths not listed in `exports` |
| Import cycle error | Reference cycle error with `tsc -b` |
| Build cache | `.tsbuildinfo` |
