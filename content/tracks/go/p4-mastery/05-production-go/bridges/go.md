---
from: go
status: draft
concept: "Operating Go services, then and now"
from_way: "Vendored dependencies checked by hand, nancy or Snyk for vulnerabilities, automaxprocs and a memory ballast in every service, gox for cross-compiling, go-bindata for assets, and filepath.Clean checks against path traversal."
native_way: "go.sum and the checksum database, govulncheck, runtime defaults that read container limits (GOMAXPROCS 1.25) and GOMEMLIMIT, built-in cross-compilation, //go:embed (1.16), os.Root (1.24), and go fix modernizers (1.26)."
sensei_tip: "Each of those old tools solved a real problem that the toolchain now solves itself. Removing them is usually the first and safest cleanup when you upgrade an old service."
---

| Older Go | Go 1.27 |
| --- | --- |
| go-bindata / packr for static files | `//go:embed` |
| gox | `GOOS=linux GOARCH=arm64 go build` |
| nancy, Snyk | `govulncheck` |
| automaxprocs + ballast | container-aware GOMAXPROCS + `GOMEMLIMIT` |
| `filepath.Clean` and prefix checks | `os.OpenRoot(dir)` (1.24) |
| reading release notes and fixing by hand | `go fix ./...` modernizers |
| behaviour changes breaking builds | GODEBUG defaults tied to the go line |
