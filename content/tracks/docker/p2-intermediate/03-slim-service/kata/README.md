# Slim Service

The starter `Dockerfile` builds and runs the Go web service from Checkpoint 1, but the image
is about 300 MB: it carries the Go toolchain, the source and a full Alpine userland, and the
server runs as root. Rewrite it as a **multi-stage build**:

| Rule | How it is checked |
| --- | --- |
| at least two stages (`FROM ... AS build`, then a runtime stage) | the Dockerfile |
| the final image is at most **30 MB** | `Size` of the image |
| no Go toolchain in the final image | `/usr/local/go` is absent |
| no source code in the final image | no `main.go` or `go.mod` anywhere |
| the server runs as a **non-root** user | `Config.User` is set and is not `root` or `0` |
| `PORT=8080` and `GREETING_NAME=dojo` still set, port 8080 exposed | `Config.Env`, `Config.ExposedPorts` |
| exec-form start command | `Config.Entrypoint` or `Config.Cmd` is a JSON array |
| it still works | `GET /health` and `GET /hello` |

Good runtime bases: `gcr.io/distroless/static-debian13:nonroot` (no shell, user 65532
already set, about 2 MB) or `alpine:3.24` with a user you create. Build the binary with
`CGO_ENABLED=0` so it does not need a C library at run time.

There are 9 tests: 1 in `tests/visible.bats` and 8 hidden ones, plus hadolint.
