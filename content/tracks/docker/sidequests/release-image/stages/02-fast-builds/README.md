Every commit rebuilds the whole image: `COPY . .` sends the docs, the scripts and the test
data (new in this stage) to the build, and any change to any of them throws the compiler's
work away. Make a rebuild after a one-line change take seconds.

1. A `.dockerignore` that keeps `.git`, `docs/`, `scripts/`, `testdata/` and Markdown
   (`*.md`) out of the build context.
2. No `COPY . .`: copy `go.mod` first, then `cmd/` and `internal/`, each on its own line.
   With dependencies you would copy `go.sum` too and run `go mod download` between the two,
   so the module download is cached until they change.
3. Keep the compiler cache between builds with a BuildKit cache mount:
   `RUN --mount=type=cache,target=/root/.cache/go-build go build ...`. A cache mount is not
   part of any layer, so it never reaches the image.

The dojo reads your `Dockerfile` and `.dockerignore` and checks that the image has none of
the new files. Measure it yourself: build, change one line of `main.go`, and build again with
`docker build --progress plain .` to see which steps say `CACHED`.
