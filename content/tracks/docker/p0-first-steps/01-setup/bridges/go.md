---
from: go
status: draft
concept: "Packaging an application with its runtime"
from_way: "Go already compiles to a single binary, so deployment is often just copying it to a server and running it under systemd."
native_way: "The image adds what the binary alone does not carry: CA certificates, time zone data, a non-root user, the start command, health checks and a standard way to ship and run it."
sensei_tip: "A static Go binary (CGO_ENABLED=0) needs almost nothing, which makes Go the easiest language to put in a tiny distroless or scratch image."
---

| Go | Docker |
| --- | --- |
| `GOOS=linux go build` | a build stage `FROM golang:1.27-alpine` |
| `scp server host:` | `docker push` to a registry, `docker pull` on the host |
| a systemd unit | the container's `ENTRYPOINT`, restart policy and health check |
| `go.mod` pins dependencies | the image pins the whole runtime environment |
