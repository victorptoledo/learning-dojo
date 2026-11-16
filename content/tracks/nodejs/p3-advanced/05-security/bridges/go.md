---
from: go
status: draft
concept: "Security habits that transfer"
from_way: "Explicit structs decoded with DisallowUnknownFields, database/sql placeholders, RE2 regular expressions that cannot backtrack, govulncheck, and subtle.ConstantTimeCompare."
native_way: "Schemas that reject unknown keys, query placeholders, backtracking regular expressions that can be abused (ReDoS), npm audit, and crypto.timingSafeEqual."
sensei_tip: "Go's RE2 engine makes ReDoS impossible; V8's regular expressions backtrack, so the pattern you write matters. Go's small standard-library-first culture is the best defence against npm supply-chain risk."
---

| Go | Node.js |
| --- | --- |
| `dec.DisallowUnknownFields()` | `.strict()` schema |
| RE2 (linear time) | Backtracking regex: watch for ReDoS |
| `govulncheck` | `npm audit` |
| `subtle.ConstantTimeCompare` | `crypto.timingSafeEqual` |
| `filepath.IsLocal` / `os.Root` | `path.resolve` + prefix check |
| `crypto/rand` | `crypto.randomBytes` |
