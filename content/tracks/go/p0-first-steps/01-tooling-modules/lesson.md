# Go 1.27, the go Command and Modules

Go was designed at Google around 2007 by Robert Griesemer, Rob Pike and Ken Thompson, and
published in 2009. Its goal was not a clever language but a **productive** one for large code
bases and large teams: fast builds, one way to format code, a small language that fits in your
head, concurrency built in, and a single static binary to deploy. Docker, Kubernetes,
Terraform, Prometheus and most of the cloud native world are written in it.

## Releases and the compatibility promise

A new Go version comes out **every six months**, in February and August. Each of the two
newest major releases gets security and bug fix releases (patch versions such as `1.27.4`).

| Release | Date | Some headline changes |
| --- | --- | --- |
| Go 1.22 | Feb 2024 | Loop variables are per iteration, `range` over integers, ServeMux patterns with methods and wildcards |
| Go 1.23 | Aug 2024 | Range over functions (iterators), the `iter` package |
| Go 1.24 | Feb 2025 | Generic type aliases, Swiss table maps, `testing.B.Loop`, `tool` directives in go.mod |
| Go 1.25 | Aug 2025 | Container-aware `GOMAXPROCS`, `sync.WaitGroup.Go`, `testing/synctest` |
| Go 1.26 | Feb 2026 | `new(expr)`, the Green Tea garbage collector by default, `errors.AsType`, a rewritten `go fix` |
| **Go 1.27** | Aug 2026 | Generic methods, `encoding/json/v2`, the `uuid` package, the goroutine leak profile |

The dojo teaches **Go 1.27**. The **Go 1 compatibility promise** says that a program written
for any Go 1.x keeps compiling and working with later 1.x releases. That is why code from
2012 still builds, and why upgrading Go is usually a one-line change. There will be no "Go 2"
that breaks your code.

## Installing Go

On Windows: `winget install GoLang.Go` (or the installer from go.dev/dl). On macOS
`brew install go`, on Linux the tarball from go.dev/dl. Then check:

```console
$ go version
go version go1.27.4 windows/amd64
$ go env GOPATH GOROOT GOTOOLCHAIN
```

In VS Code, install the **Go extension** (`golang.go`). It installs `gopls`, the language
server, formats on save and runs tests from the editor.

## Packages, modules and a first program

- A **package** is a folder of `.go` files that start with the same `package` clause. It is
  the unit of compilation and of visibility: names that start with an **upper case letter**
  are **exported**; everything else is private to the package.
- A **module** is a tree of packages versioned together, described by a `go.mod` file at its
  root. Its **module path** (`github.com/acme/shop`) is the prefix of every import path in it.

```console
$ mkdir hello && cd hello
$ go mod init example.com/hello
```

```go
// main.go
package main // package main + func main = an executable

import (
	"fmt"
	"os"
)

func main() {
	name := "gopher"
	if len(os.Args) > 1 {
		name = os.Args[1]
	}
	fmt.Printf("Hello, %s!\n", name)
}
```

```console
$ go run . Ada        # compile to a temporary binary and run it
Hello, Ada!
$ go build -o hello . # a single static binary, no runtime to install
```

A typical layout: `cmd/<app>/main.go` for each executable, packages beside it, and
`internal/` for packages that **only this module** may import (the compiler enforces it).

## go.mod and go.sum

```text
module example.com/shop

go 1.26.0              // minimum Go version: language features and requirements

toolchain go1.27.4     // preferred toolchain when the local one is older

require (
	github.com/google/go-cmp v0.7.0
	golang.org/x/sync v0.18.0 // indirect
)

tool golang.org/x/tools/cmd/stringer   // a tool run with `go tool stringer`
```

- **`go`** is a requirement, not a hint: a Go 1.25 toolchain refuses to build a module that
  says `go 1.26.0`, and the language features available follow this line.
- **`require`** lists every module the build needs; `// indirect` marks ones no package of
  yours imports directly. Go uses **minimal version selection**: it picks the oldest version
  that satisfies every requirement, so builds are reproducible without a separate lock file.
- **`go.sum`** holds the cryptographic hash of every module version used. The go command
  checks downloads against it and against the public checksum database (`sum.golang.org`).
  **Commit both files.**
- **`tool`** (Go 1.24) records development tools in go.mod instead of a `tools.go` file.

> [!NOTE]
> Since Go 1.26, `go mod init` writes the **previous** minor version (`go 1.26.0` with a 1.27
> toolchain), so new modules work for people one release behind. Run `go get go@1.27.0` to
> require 1.27 features such as generic methods.

### Dependencies

```console
$ go get github.com/google/go-cmp@v0.7.0   # add or upgrade one module
$ go get -u ./...                          # upgrade dependencies of every package
$ go get -tool golang.org/x/tools/cmd/stringer@latest
$ go mod tidy                              # add what imports need, drop what they don't
```

Modules are downloaded through a proxy (`GOPROXY`, by default `proxy.golang.org`) into the
module cache. A major version 2 or later changes the import path (`example.com/lib/v2`), so
two major versions can coexist in one build.

### Toolchain selection

Since Go 1.21, an older `go` command that meets a module needing a newer Go **downloads the
right toolchain** and runs it (`GOTOOLCHAIN=auto`, the default). `GOTOOLCHAIN=local` forbids
that, which is what the dojo's sandbox does.

## The go command

| Command | What it does |
| --- | --- |
| `go run .` | Build and run the package in the current folder |
| `go build ./...` | Compile every package (and catch type errors) |
| `go test ./...` | Run the tests; `-race` adds the race detector, `-run` filters by name |
| `go vet ./...` | Report suspicious code: wrong `Printf` verbs, copied locks, unreachable code |
| `gofmt -w .` / `go fmt ./...` | Format the code the one standard way |
| `go fix ./...` | Apply the **modernizers** (Go 1.26): rewrite old idioms into current ones |
| `go doc strings.Cut` | Read documentation from the terminal |
| `go mod tidy` | Make go.mod and go.sum match the imports |
| `go install pkg@version` | Build and install a command into `$GOPATH/bin` |

Formatting is not a style debate in Go: **every Go file is gofmt-formatted**. Configure your
editor to format on save and never think about it again. Unused imports and unused local
variables are **compile errors**, not warnings.

## How the dojo checks Go katas

Each Go kata is a module with one package. You edit the `.go` files; functions still to write
call `panic("todo ...")`. When you save, the dojo copies your folder into the `dojo-go`
container (Go 1.27, no network, standard library only), adds the hidden grading tests next to
yours, and runs:

1. `go build`: the code must compile;
2. the tests, with the **race detector** on;
3. `go vet` on the package and `gofmt` on every source file.

Run the visible tests yourself with `go test` (or `go test -race`) if Go is installed.

## Summary

- Go is a small, compiled, garbage-collected language with built-in concurrency, released
  every six months and backed by the Go 1 compatibility promise.
- A package is a folder; a module is a versioned tree of packages described by go.mod.
- go.mod holds the module path, the minimum `go` version, `toolchain`, `require` and `tool`;
  go.sum holds checksums. Commit both; `go mod tidy` keeps them right.
- gofmt, go vet and go test are part of everyday work; go fix modernises old code.

## Further reading

- Tutorial, "Get started with Go": <https://go.dev/doc/tutorial/getting-started>
- How to write Go code (packages, modules, tests): <https://go.dev/doc/code>
- Go Modules Reference: <https://go.dev/ref/mod>
- Go toolchains and GOTOOLCHAIN: <https://go.dev/doc/toolchain>
- Go 1 and the future of Go programs (the compatibility promise): <https://go.dev/doc/go1compat>
- Go 1.27 release notes: <https://go.dev/doc/go1.27>
- roadmap.sh, Go topic "go mod": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/go-mod@5hnaPrYSBzxEIxFeg5tCK.md>
