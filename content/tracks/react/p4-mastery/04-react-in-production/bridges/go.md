---
from: go
status: draft
concept: "Simple structure, strong tooling"
from_way: "Go projects use packages by domain with internal/ to hide internals, gofmt and go vet as non-negotiable tooling, and go fix for upgrades."
native_way: "React projects use feature folders with public index files (like internal/), formatter and lint rules in CI, and codemods for upgrades."
sensei_tip: "Copy Go's habits: an internal/-like boundary per feature, tooling that runs on every commit with no exceptions, and small incremental upgrades."
---

| Go | React |
| --- | --- |
| Package per domain | Feature folder |
| `internal/` | Import only through a feature's `index.ts` |
| `gofmt`, `go vet` in CI | Prettier, ESLint, tsc in CI |
| `go fix` | React codemods |
| pprof in production | Web Vitals and the Profiler |
| Structured error logging | Error boundaries + monitoring |
