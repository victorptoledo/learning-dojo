---
from: go
status: draft
concept: "Incremental builds and dependency caching"
from_way: "The module cache and the build cache in $GOPATH make repeated go build calls fast on a laptop."
native_way: "In a Dockerfile, COPY go.mod go.sum, RUN go mod download, then COPY the sources and build; Phase 3 adds cache mounts for the build cache itself."
sensei_tip: "go mod download in its own layer means a code change never re-downloads modules, even without network access to a proxy."
---

| Go | Dockerfile |
| --- | --- |
| `go.mod` and `go.sum` | copied first |
| `go mod download` | a cached layer |
| `GOFLAGS=-trimpath` | reproducible binaries independent of paths |
| `go vet` | hadolint for the Dockerfile |
