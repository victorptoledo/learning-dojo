# Go in Production: Builds, Containers, Configuration and Security

Go was built for running services: one static binary, fast start-up, small images, and a
toolchain that also checks your dependencies for known vulnerabilities. This lesson collects
what a production Go service needs beyond its code.

## Layout of a service

```text
shop/
├── go.mod
├── cmd/
│   └── shop-api/main.go      # wiring: config, logger, dependencies, server, shutdown
├── internal/                 # everything else: only this module can import it
│   ├── orders/               # packages by domain, not by layer
│   ├── payments/
│   └── platform/postgres/
└── Dockerfile
```

Keep `main` thin: it reads the configuration, builds the dependencies with `NewX`
functions, starts the server and handles shutdown. Name packages by what they provide
(`orders`, `postgres`), not `utils` or `common`.

## Builds

```console
$ CGO_ENABLED=0 go build -trimpath -ldflags="-s -w -X main.version=1.4.2" -o bin/shop-api ./cmd/shop-api
$ GOOS=linux GOARCH=arm64 go build ./cmd/shop-api     # cross-compile, no extra toolchain
$ go version -m bin/shop-api                         # the modules and settings it was built with
```

- `CGO_ENABLED=0` gives a fully static binary that runs on `scratch` or distroless images.
- `-trimpath` removes local paths; `-ldflags="-s -w"` drops debug symbols (smaller binary);
  `-X` sets a string variable such as the version.
- The binary embeds its **build info** (module versions, VCS revision, settings), readable
  at run time with `debug.ReadBuildInfo()` and by scanners.
- `//go:embed` puts templates, migrations or static files inside the binary.

## Container images

```dockerfile
FROM golang:1.27 AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download                      # cached layer while dependencies do not change
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/shop-api ./cmd/shop-api

FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /out/shop-api /shop-api
USER nonroot
ENTRYPOINT ["/shop-api"]
```

The final image holds the binary, CA certificates and little else: a few megabytes, no
shell, nothing to patch but your code. Import `time/tzdata` if the program needs time zones
and the image has none.

In the container the runtime already does the right thing: `GOMAXPROCS` follows the CPU limit
(Go 1.25). Set `GOMEMLIMIT` a little under the memory limit, from the deployment.

## Configuration

Follow the twelve-factor habit: configuration comes from the **environment** (and flags for
local runs), is parsed **once at start-up** into a typed struct, and the program **refuses to
start** when it is invalid:

```go
type Config struct {
	Addr        string
	DatabaseURL string
	ReadTimeout time.Duration
}

func LoadConfig() (Config, error) {
	cfg := Config{Addr: ":8080", ReadTimeout: 10 * time.Second}
	if v := os.Getenv("ADDR"); v != "" {
		cfg.Addr = v
	}
	cfg.DatabaseURL = os.Getenv("DATABASE_URL")
	if cfg.DatabaseURL == "" {
		return Config{}, errors.New("DATABASE_URL is required")
	}
	if v := os.Getenv("READ_TIMEOUT"); v != "" {
		d, err := time.ParseDuration(v)
		if err != nil {
			return Config{}, fmt.Errorf("READ_TIMEOUT: %w", err)
		}
		cfg.ReadTimeout = d
	}
	return cfg, nil
}
```

Secrets come from the environment or a secret manager, never from the repository, and never
reach the logs.

## Lifecycle

A well-behaved service, recapping the earlier lessons:

1. Loads and validates its configuration, builds a `slog` logger.
2. Opens dependencies with timeouts (database pool sizes set explicitly).
3. Serves with an `http.Server` that has timeouts; `/healthz` and `/readyz` for the
   orchestrator; pprof and metrics on an internal port.
4. On SIGTERM (`signal.NotifyContext`), reports not ready, calls `srv.Shutdown` with a
   deadline shorter than the platform's grace period, closes dependencies and exits 0.

## Security

- **`govulncheck ./...`** reports known vulnerabilities from the Go vulnerability database,
  and only those your code **actually calls**, so the list is short and real. Run it in CI and
  on the built binary (`govulncheck -mode=binary`).
- Dependencies are verified by **go.sum** and the checksum database; private modules go in
  `GOPRIVATE`. CI builds with `-mod=readonly` (the default) so go.mod cannot change silently.
- Update regularly: `go get -u ./...`, `go mod tidy`, tests, and a bot for pull requests.
- In code: parameterised SQL (`db.QueryContext(ctx, "... WHERE id = $1", id)`), `html/template`
  for HTML (it escapes by context), `os.Root` (Go 1.24) to keep file access inside a
  directory, `crypto/rand` for anything secret. TLS defaults are modern; since Go 1.26 they
  include post-quantum hybrid key exchange.
- Static analysis in CI: `go vet`, **staticcheck** or **golangci-lint**, and tests with
  `-race`.

## Staying current

Go supports the **two newest major releases**. Upgrading is usually easy thanks to the
compatibility promise:

1. Raise the `go` line in go.mod (and the toolchain in CI and the Dockerfile).
2. Run the tests with `-race`, then `go fix ./...` to apply the **modernizers**, and review
   the diff.
3. Read the release notes for behaviour changes. Changes that could break programs are
   guarded by **GODEBUG** settings tied to the `go` line, so an old module keeps its old
   behaviour until you raise the line; a `godebug` block in go.mod can pin one setting while
   you adapt.

## Summary

- `cmd/` for binaries, `internal/` for the rest, packages by domain; a thin `main`.
- Static, trimmed binaries with version info; multi-stage images on distroless, non-root.
- Configuration from the environment into a validated struct; secrets never in code or logs.
- Timeouts, health endpoints, graceful shutdown, `GOMEMLIMIT` in containers.
- `govulncheck`, go.sum, regular updates, safe APIs and static analysis in CI.
- Upgrade every release: go line, tests, `go fix`, GODEBUG for compatibility.

## Further reading

- Organizing a Go module: <https://go.dev/doc/modules/layout>
- Go vulnerability management and govulncheck: <https://go.dev/doc/security/vuln/>
- Security best practices for Go developers: <https://go.dev/doc/security/best-practices>
- Go, Backwards Compatibility, and GODEBUG: <https://go.dev/doc/godebug>
- Package embed: <https://pkg.go.dev/embed>
- roadmap.sh, Go topic "govulncheck": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/govulncheck@K__satcG2ETNRFBpPqrLw.md>
