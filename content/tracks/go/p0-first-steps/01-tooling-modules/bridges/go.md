---
from: go
status: draft
concept: "Modules and the toolchain since GOPATH"
from_way: "Code under $GOPATH/src, dep or glide for dependencies, a tools.go file with blank imports for tools, vendor folders everywhere and go get used to install commands."
native_way: "Modules anywhere on disk, go.mod with go, toolchain and tool directives, automatic toolchain downloads, go install pkg@version for commands and go fix to modernise code."
sensei_tip: "Run go fix ./... once on an old code base after raising the go line: the modernizers rewrite loops, min and max, slices and maps helpers and more without changing behaviour."
---

| Older Go | Go 1.27 |
| --- | --- |
| `$GOPATH/src/github.com/me/app` | any folder with a `go.mod` |
| `dep`, `glide`, `Gopkg.toml` | `go.mod` + `go.sum` |
| `go get github.com/x/cmd` to install a tool | `go install github.com/x/cmd@v1.2.3` |
| `tools.go` with `_` imports | `tool` directives and `go tool name` (1.24) |
| "install Go 1.x on every machine" | `toolchain` line and `GOTOOLCHAIN=auto` (1.21) |
| `go tool doc` | `go doc` (1.26) |
| obsolete `go fix` rewrites | `go fix` modernizers (1.26) |
