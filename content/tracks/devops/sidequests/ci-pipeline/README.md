# Sidequest: CI for a script toolbox

Your team keeps a toolbox of small bash scripts in `bin/`, with bats tests in `test/`.
Nobody runs the tests, so they break. You give it a continuous integration pipeline in
three stages:

1. `local-checks`: `scripts/ci.sh`, one entry point that lints and tests the toolbox. CI
   calls the same script you run on your laptop, so "works on my machine" means "passes
   in CI";
2. `workflow`: a GitHub Actions workflow around it, with the right triggers, a read-only
   token, cancelled stale runs, timeouts and a cache for the tools;
3. `matrix`: the tests on several runners, their JUnit reports kept as artifacts, and a
   single `ci-ok` job for the branch protection rule to require.

How it is checked:

- Stage 1 runs your `ci.sh` for real: the tests copy the repository to a temporary folder,
  break a script or a test on purpose and read what `ci.sh` prints, its exit status and
  the report it writes.
- Stages 2 and 3 read `.github/workflows/ci.yml` with `yq` and lint it with `actionlint`
  (which also runs ShellCheck on every `run:` script). Third-party actions are pinned to a
  full commit SHA. A few `run:` scripts are executed by the tests, as stated.
- Every `.sh` file must pass ShellCheck. The tests run in Debian 13 without network, with
  `shellcheck`, `bats`, `yq` and `jq`.

Useful reading: the GitHub Actions docs on `concurrency`, `permissions`, `actions/cache`,
`strategy.matrix` and the `needs` context. When you are done, `[A]` copies a review prompt:
ask an AI about your pipeline.
