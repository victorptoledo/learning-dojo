# CI Pipelines: Triggers, Jobs, Caches, Artefacts and Branching

A CI pipeline is the automated gate every change passes: it builds the code, runs the checks
and produces the artefact that will be deployed. A good pipeline is **fast** (minutes, not
hours, so people keep batches small), **trustworthy** (green means good, red means a real
problem) and **reproducible** (the same commit gives the same result). This lesson uses
GitHub Actions; GitLab CI, Jenkins, Azure Pipelines and CircleCI use the same ideas with
other words.

## The anatomy of a workflow

```yaml
# .github/workflows/ci.yml
name: ci
on:                                   # triggers
  push:
    branches: [main]
  pull_request:
permissions:                          # what the GITHUB_TOKEN may do
  contents: read
concurrency:                          # one run per ref; a newer push cancels the older run
  group: ci-${{ github.ref }}
  cancel-in-progress: true
jobs:
  test:                               # a job: a fresh runner (VM or container)
    runs-on: ubuntu-24.04
    timeout-minutes: 15
    strategy:
      matrix:
        go: ["1.26", "1.27"]          # one job per value, in parallel
    steps:                            # steps run in order on that runner
      - uses: actions/checkout@v7     # an action: reusable code
      - uses: actions/setup-go@v7
        with: { go-version: "${{ matrix.go }}" }
      - run: go test -race ./...      # a shell command
  image:
    needs: [test]                     # runs after test succeeds
    if: github.event_name == 'push'
    runs-on: ubuntu-24.04
    steps: [...]
```

| Concept | GitHub Actions | GitLab CI | Jenkins |
| --- | --- | --- | --- |
| pipeline file | `.github/workflows/*.yml` | `.gitlab-ci.yml` | `Jenkinsfile` |
| unit of parallelism | job | job (in stages) | stage / parallel branch |
| ordering | `needs` | `stages`, `needs` | sequential stages |
| reusable logic | actions, reusable workflows | `include`, components | shared libraries |
| machines | runners | runners | agents |

Jobs do **not** share a file system: each starts on a clean machine. Pass files between jobs
as **artefacts** (`actions/upload-artifact` / `download-artifact`), and outputs as
`outputs:` values.

## Triggers

`push` and `pull_request` cover most CI. Others: `schedule` (cron, for nightly scans),
`workflow_dispatch` (a manual button with inputs), `release`, `workflow_call` (called by
another workflow) and `pull_request_target` (runs in the context of the base repository with
secrets: dangerous with fork code, see Phase 3). `paths` and `branches` filters limit when a
workflow runs.

## Fast pipelines

- **Parallelise**: independent jobs run at the same time; `needs` only for real dependencies.
- **Cache** dependency downloads, keyed by the lock file's hash
  (`actions/cache`, or the `cache:` input of setup actions), and Docker layers
  (`cache-from: type=gha`).
- **Fail fast on cheap checks**: lint and unit tests before slow integration tests.
- **Test what changed** in monorepos (path filters, Nx, Bazel, Turborepo), but keep the
  required checks stable so branch protection still works.
- **Cancel superseded runs** with `concurrency`.
- Aim for under ten minutes from push to result; beyond that people stop waiting and start
  batching.

## Trustworthy pipelines

- **Least privilege**: `permissions: { contents: read }` at the top, widened per job only where
  needed (`packages: write` to push an image, `id-token: write` for cloud OIDC).
- **Pin** actions to a commit SHA and tools to exact versions: a moving tag is someone else's
  code changing under you. Dependabot or Renovate keeps the pins fresh.
- **Timeouts** on every job, so a hung test does not burn hours.
- **Flaky tests** destroy trust: quarantine them in a separate, non-blocking job and fix them;
  automatic retries hide real races.
- **Hermetic builds**: lock files, pinned base images and no "latest" downloads make a commit's
  build repeatable.

## Branching that keeps CI meaningful

CI means integrating continuously, so the branching model matters:

| Model | How | Fits |
| --- | --- | --- |
| **Trunk-based development** | short-lived branches (hours to a day or two) merged to main behind feature flags | most teams; the DORA research links it to high performance |
| **GitHub flow** | a branch per change, a pull request, merge to main, deploy main | web services |
| **GitFlow** | develop, feature, release and hotfix branches | versioned products with several supported releases; heavy for services |

Protect `main`: require pull requests, the CI checks and at least one review, and forbid
force pushes. A **merge queue** tests each pull request on top of the latest main before
merging, so main stays green even with many merges an hour.

## Summary

- A workflow has triggers, permissions, jobs (parallel, on fresh runners) and ordered steps;
  `needs`, matrices and artefacts connect them.
- Make it fast: parallel jobs, caches keyed by lock files, cheap checks first, cancelled
  superseded runs.
- Make it trustworthy: least-privilege tokens, pinned actions and tools, timeouts, no flaky
  tests.
- Trunk-based development and a protected main with required checks keep integration
  continuous.

## Further reading

- GitHub Actions workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>
- Security hardening for GitHub Actions: <https://docs.github.com/en/actions/reference/security/secure-use>
- GitLab CI/CD YAML reference: <https://docs.gitlab.com/ci/yaml/>
- Trunk-based development: <https://trunkbaseddevelopment.com/>
- actionlint: <https://github.com/rhysd/actionlint>
