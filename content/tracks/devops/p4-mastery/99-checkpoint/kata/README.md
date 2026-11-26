# 🥋 Checkpoint 4: The Golden Path

You lead the new platform team. Creating a service takes three weeks, and every team copies
the first service's files: a workflow with stored AWS keys, an alert on "more than one error
per second", daily snapshots in the same region and a runbook that says "read the logs until
you find the cause". Build the **golden path** instead: one command that creates a new
service with everything the track taught, ready for production on day one.

```
./new-service.sh --name NAME --team TEAM --tier critical|standard [--out DIR]
```

It creates `DIR/NAME/` (`DIR` defaults to `.`) with exactly these files:

```
.github/workflows/ci.yml       from templates/ci.yml.tpl
slo/NAME.rules.yml             from templates/slo.rules.yml.tpl
RUNBOOK.md                     from templates/RUNBOOK.md.tpl
catalog-info.yaml              from templates/catalog-info.yaml.tpl (done)
infra/main.tf                  a copy of templates/infra/main.tf
infra/terraform.tfvars         service, team and tier (see below)
```

## The script (`new-service.sh`)

| Rule | Detail |
| --- | --- |
| validate everything **before** writing anything | `--name`: 3 to 30 of `a-z`, `0-9` and `-`, starting with a letter and not ending with `-`; `--team`: `a-z`, `0-9` and `-`, starting with a letter; `--tier`: `critical` or `standard`; all three required; any other option is an error |
| bad input | exit **2**, a message on stderr, nothing written |
| `DIR/NAME` already exists | exit **1** and leave it untouched: the golden path never overwrites a service |
| a failure halfway (a template missing, a full disk) | leaves nothing behind: build the tree in a temporary folder inside `DIR` and `mv` it in place at the end |
| the same input | gives the same bytes every time (no dates, no random names in the files) |
| placeholders | `{{name}}`, `{{team}}`, `{{tier}}`, `{{slo}}`, `{{budget}}`, `{{rpo}}` and `{{rto}}` are all filled; none is left |
| `infra/terraform.tfvars` | `service = "NAME"`, `team    = "TEAM"` and `tier    = "TIER"`, one per line, aligned like `terraform fmt` does |

| Tier | `slo` | `budget` | `rpo` | `rto` |
| --- | --- | --- | --- | --- |
| `critical` | `99.9` | `0.001` | `15 minutes` | `2 hours` |
| `standard` | `99.5` | `0.005` | `24 hours` | `8 hours` |

Write durations as people read them: `15 minutes`, `1 hour`, `2 hours`, `24 hours`.

## The templates

| Template | Rule |
| --- | --- |
| `ci.yml.tpl` | runs on pushes to `main` and on pull requests; top-level `permissions: { contents: read }`; workflow `concurrency` `ci-${{ github.ref }}` that cancels only pull request runs (`cancel-in-progress: ${{ github.event_name == 'pull_request' }}`); every job has `timeout-minutes`; every action pinned to a SHA; `actions/checkout` with `persist-credentials: false` |
| `ci.yml.tpl` | job `test` runs `make test`; job `deploy` `needs: test`, runs only for pushes to `main`, in the `production` environment, with its own `concurrency` group `deploy-NAME` that is **never** cancelled, `permissions: { contents: read, id-token: write }`, and assumes `arn:aws:iam::123456789012:role/deploy-NAME` in `eu-west-1` through OIDC (no stored keys, no `secrets.` at all), then runs `make deploy SERVICE=NAME` |
| `slo.rules.yml.tpl` | the availability SLO of checkpoint-3 style: `job:slo_errors_per_request:ratio_rate5m`, `30m`, `1h` and `6h` (5xx rate over total rate of `http_requests_total{job="NAME"}`, `sum by (job)`), and only two alerts: `ErrorBudgetBurnFast` (1h and 5m above `14.4 * budget`, `for: 2m`, `severity: page`) and `ErrorBudgetBurnSlow` (6h and 30m above `6 * budget`, `for: 15m`, `severity: ticket`) |
| `slo.rules.yml.tpl` | both alerts carry the labels `service: NAME`, `team: TEAM` and `slo: NAME-availability`, the `summary` `NAME is burning its error budget fast` (or `... its error budget` for the slow one) and `runbook_url: https://runbooks.example.com/NAME` |
| `RUNBOOK.md.tpl` | starts with `# NAME runbook`, an empty line, then `- Owner: team TEAM`, `- Tier: TIER`, `- SLO: SLO% of requests succeed over 30 days`; then the sections `## Severity`, `## Roles` (lines `- Incident commander: ...`, `- Operations: ...`, `- Communications: ...`), `## First five minutes` (numbered steps: **roll back** the most recent change before you **diagnose**), `## Restore from backup` (lines `- RPO: ...` and `- RTO: ...`) and `## After the incident` (a blameless postmortem) |
| `infra/main.tf` | validates `service` like `--name`, `team` like `--team` and `tier`; outputs `backup_policy` from the table below and `tags` = `service`, `team`, `tier` and `managed-by = "terraform"` |

| `backup_policy` | `critical` | `standard` |
| --- | --- | --- |
| `rpo_minutes` / `rto_minutes` | 15 / 120 | 1440 / 480 |
| `point_in_time_recovery` | `true` | `false` |
| `cross_region_copy` / `immutable` | `true` / `true` | `true` / `true` |
| `retention_days` | 35 | 14 |
| `restore_test_every_days` | 30 | 90 |

Both tiers keep immutable copies in another region: a region outage, a bad script or
ransomware must not take the backups with the data.

The dojo runs ShellCheck on the script and `terraform fmt` and `validate` on the template
module. Then the hidden tests generate a critical service (`orders`, team `checkout`) and a
standard one (`search`, team `discovery`) and check **what you generated** with the track's
own tools: actionlint and the workflow policies, `promtool check` and `promtool test rules`
scenarios at each tier's thresholds, `terraform validate` and `terraform test` in the
generated `infra/`, and bats for the runbook and the catalog.

There are 29 tests: 2 in `tests/visible.bats`, 24 hidden bats tests, ShellCheck, `fmt` and
`validate`.
