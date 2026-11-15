---
from: go
status: draft
concept: "Layered, content-addressed artifacts"
from_way: "go.sum records a hash for every module version, and the module cache stores them content-addressed."
native_way: "An image applies the same content addressing to file system layers: a digest identifies bytes, a tag is a name you can move."
sensei_tip: "A Go service image can be two layers: a tiny base and the binary. Then every release ships a single new layer of a few megabytes."
---

| Go | Image |
| --- | --- |
| `go.sum` hashes | layer digests |
| a module version `v1.4.0` | a tag; pin `@sha256:` for exact bytes |
| the module cache `GOMODCACHE` | the engine's layer store |
| `go mod download` | `docker pull` of the missing layers |
