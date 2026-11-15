---
from: go
status: draft
concept: "Getting and managing a development machine"
from_way: "go install and modules fetch dependencies per project, and the toolchain comes from go.dev; a Go program compiles to one static binary."
native_way: "apt installs system packages, but Go programs usually need none of them: copy the single binary, run it under systemd or in a tiny container."
sensei_tip: "Build with CGO_ENABLED=0 and the binary runs on any Linux, even in a FROM scratch image. The golang package in Debian lags behind; use the official tarball or image for builds."
---

| Go | Linux |
| --- | --- |
| go install tool@latest | sudo apt install <tool>, or go install into ~/go/bin |
| go.mod dependencies | unchanged; nothing to install on the server |
| GOOS=linux go build | the binary you copy to the server |
| a single binary | plus a systemd unit and a user to run it |
