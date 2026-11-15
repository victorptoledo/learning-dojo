---
from: go
status: draft
concept: "Versions and release tags"
from_way: "Hoping users pick up your latest commit."
native_way: "In Go the tag is the release: go get module@v1.5.0 resolves the Git tag v1.5.0, and semver is mandatory."
sensei_tip: "A major version 2 or higher needs /v2 in the module path as well as the v2.0.0 tag. Tagging v2.0.0 without changing go.mod makes the version unusable (+incompatible)."
---

| Go modules | Git |
| --- | --- |
| `v1.5.0` | tag `v1.5.0` |
| module in a subdirectory `api/` | tag `api/v1.5.0` |
| `retract v1.4.0` in go.mod | leave the bad tag, release a fixed one |
| pseudo-version | a commit with no tag |
