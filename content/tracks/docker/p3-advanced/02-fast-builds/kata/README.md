# Fast Builds

The starter `Dockerfile` is a working multi-stage build of the Go service, but:

- it sends the whole folder to the builder, including `docs/` (design notes) and
  `tests/`, and copies everything before building, so **any** change, even to
  a Markdown file, recompiles the service from scratch;
- Go's compiler cache is thrown away after each build;
- the version is declared with `ARG VERSION=1.4.0` but never used: `server -version` prints
  `dev` and the image has no version label.

Fix it:

| Rule | How it is checked |
| --- | --- |
| `go.mod` is copied before the `.go` files, and no `COPY . .` in the build stage | the Dockerfile |
| `go build` runs with a cache mount on `/root/.cache/go-build` | the Dockerfile |
| `.dockerignore` keeps `.git`, `docs/`, `tests/` and Markdown files out of the context | the file |
| `docker run IMAGE -version` prints `1.4.0` (pass the argument with `-ldflags "-X main.version=..."`) | the output |
| the final image has the label `org.opencontainers.image.version=1.4.0`, from the same `ARG` | `Config.Labels` |
| still small (at most 30 MB), non-root, and answering `/health` | `Size`, `Config.User`, HTTP |

`main.version` is a variable in `main.go`; `go build -ldflags "-X main.version=1.4.0"` sets it
at link time.

Try the difference yourself: build twice with `docker build -t fast .`, change a line of
`main.go`, build again, and look for `CACHED` in the output.

There are 9 tests: 1 in `tests/visible.bats` and 8 hidden ones, plus hadolint.
