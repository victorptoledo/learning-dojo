---
from: go
status: draft
concept: "Build caches, build secrets and target platforms"
from_way: "GOCACHE and GOMODCACHE make local builds fast, GOPRIVATE with a .netrc reaches private modules, and GOOS/GOARCH cross-compile."
native_way: "Cache mounts for /root/.cache/go-build and /go/pkg/mod, a secret mount for .netrc, and FROM --platform=$BUILDPLATFORM with GOOS=$TARGETOS GOARCH=$TARGETARCH."
sensei_tip: "Go cross-compiles natively, so multi-platform Go images build at full speed without QEMU when the build stage runs on BUILDPLATFORM."
---

| Go | BuildKit |
| --- | --- |
| `GOCACHE` | `--mount=type=cache,target=/root/.cache/go-build` |
| `GOMODCACHE` | `--mount=type=cache,target=/go/pkg/mod` |
| `.netrc` for private modules | `--mount=type=secret,id=netrc,target=/root/.netrc` |
| `GOOS`/`GOARCH` | `TARGETOS`/`TARGETARCH` |
