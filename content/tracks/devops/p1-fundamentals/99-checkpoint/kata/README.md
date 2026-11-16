# Checkpoint 1: A Production CI Pipeline

The `orders` service (a Go module with a `Dockerfile`) has a first-draft workflow in
`.github/workflows/ci.yml`. It runs, but it pushes `:latest` from every branch with a token
that can write everything. Rewrite it into a pipeline you would trust in production.

| Rule | How |
| --- | --- |
| runs on pushes to `main` and on every pull request | `on` |
| the token is read-only by default: `contents: read`, never `write-all` | top-level `permissions` |
| a newer push to the same ref cancels the older run | `concurrency` with a group built from `github.ref`, `cancel-in-progress: true` |
| every action is pinned to a full 40-character commit SHA (keep the version in a comment) | `uses: owner/action@<sha> # vX.Y.Z` |
| every job has a `timeout-minutes` | per job |
| every checkout leaves no token behind in `.git/config` | `persist-credentials: false` |
| job `lint`: Go from `go.mod`, then fails if `gofmt -l .` lists any file, then `go vet ./...` | `test -z "$(gofmt -l .)"` |
| job `test`: `go test -race ./...` on Go `1.26` and `1.27`, all versions run even if one fails | a matrix `go`, `fail-fast: false`, `go-version: ${{ matrix.go }}` |
| job `image`: starts only after `lint` and `test` pass | `needs` |
| only `image` may write packages: `contents: read`, `packages: write` | job `permissions`; none on the other jobs |
| it builds with Buildx and `docker/build-push-action`, pushes only on `push` events, tagged `ghcr.io/${{ github.repository }}:${{ github.sha }}` | `push: ${{ github.event_name == 'push' }}` |
| the build reuses the GitHub Actions cache | `cache-from: type=gha`, `cache-to: type=gha,mode=max` |

Pins you can use (taken from each project's latest release):

| Action | Commit | Version |
| --- | --- | --- |
| `actions/checkout` | `3d3c42e5aac5ba805825da76410c181273ba90b1` | v7.0.1 |
| `actions/setup-go` | `b7ad1dad31e06c5925ef5d2fc7ad053ef454303e` | v7.0.0 |
| `docker/setup-buildx-action` | `f87e5991a6d7451dcb8d9637bfbc97413f497069` | v4.4.1 |
| `docker/login-action` | `8aa5831fb8d48d8f76594c7ddde61ae58019f550` | v4.8.0 |
| `docker/build-push-action` | `c3c9e263c25d99ce0380d002d59b67737d91b0dc` | v7.4.0 |

Log in to `ghcr.io` (with `${{ secrets.GITHUB_TOKEN }}`) only when the image is pushed.
The dojo checks the workflow with actionlint and reads it with bats; nothing runs on GitHub.

There are 14 tests: 1 in `tests/visible.bats`, 12 hidden ones and actionlint.
