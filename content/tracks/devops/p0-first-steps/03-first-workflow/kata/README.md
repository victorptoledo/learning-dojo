# Your First CI Workflow

The `greeter` service is a Go module (`go.mod` is in the kata folder). Write its continuous
integration workflow in `.github/workflows/ci.yml`, so every change is vetted and tested
before anyone merges it.

| Rule | How |
| --- | --- |
| the workflow is named `ci` | `name` |
| it runs on pushes to `main` and on every pull request (any target branch) | `on.push.branches`, `on.pull_request` |
| the `GITHUB_TOKEN` can only read the repository | top-level `permissions` |
| one job `test` on GitHub's Ubuntu 24.04 runner | `runs-on: ubuntu-24.04` |
| step 1 checks out the code | `actions/checkout@v7` |
| then Go is installed in the version `go.mod` names | `actions/setup-go@v7` with `go-version-file: go.mod` |
| then `go vet ./...` and `go test ./...` run | two `run` steps |

The dojo does not run the workflow: it checks it with **actionlint** (the GitHub Actions
linter, which also runs ShellCheck over every `run:` script) and reads it with bats tests.
To lint it on your machine, install actionlint and run `actionlint` in the kata folder.

There are 9 tests: 1 in `tests/visible.bats`, 7 hidden ones and actionlint.
