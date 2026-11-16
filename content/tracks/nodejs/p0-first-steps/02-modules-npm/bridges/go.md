---
from: go
status: draft
concept: "Modules, versions and checksums"
from_way: "go.mod lists module requirements, minimal version selection picks the lowest allowed version, go.sum records checksums, and packages are folders."
native_way: "package.json lists ranges, npm picks the highest allowed version and records it in package-lock.json with integrity hashes, and every file is a module."
sensei_tip: "Go's minimal version selection is reproducible without a lockfile; npm is not, so the lockfile is not optional. A Node.js package is closer to a Go module, and a file to a Go package."
---

| Go | Node.js |
| --- | --- |
| `go.mod` | `package.json` |
| `go.sum` | `package-lock.json` (integrity hashes) |
| Minimal version selection | Highest version in range + lockfile |
| `import "example.com/x/util"` | `import { f } from "pkg/util"` |
| Internal packages (`internal/`) | `exports` hides unlisted files |
| `go mod download` in CI | `npm ci` |
