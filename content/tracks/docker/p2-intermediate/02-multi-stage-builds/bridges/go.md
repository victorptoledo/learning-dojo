---
from: go
status: draft
concept: "Separate build and runtime environments"
from_way: "go build on a laptop or CI agent with the toolchain, then copy the binary anywhere."
native_way: "The build stage is the golang image; the runtime stage is distroless/static or scratch with just the binary, certificates and a user."
sensei_tip: "CGO_ENABLED=0 is what makes the binary run on distroless/static or scratch; with cgo you need a glibc base such as distroless/base."
---

| Go | Multi-stage |
| --- | --- |
| the Go toolchain | `FROM golang:1.27-alpine AS build` |
| `CGO_ENABLED=0 go build` | a static binary |
| the binary | `COPY --from=build /out/server /app/server` |
| a bare server | `FROM gcr.io/distroless/static-debian13:nonroot` |
