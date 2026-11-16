---
from: go
status: draft
concept: "Integrate continuously, release in small batches"
from_way: "You tag a release when enough has piled up and build binaries by hand with GOOS and GOARCH."
native_way: "CI builds and tests every merge (GoReleaser for tags), and each build is a candidate for production."
sensei_tip: "Go's fast builds and tests make sub-five-minute CI realistic; guard that speed, it is what keeps batches small."
---

| Go world | DevOps |
| --- | --- |
| `go test ./...` before pushing | the CI gate |
| a manual tag | an automated release on merge |
| `-ldflags "-X main.version=..."` | build metadata from CI |
| a binary per platform | an image per commit |
