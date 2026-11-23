Staging and production should not share one policy: staging can go fast, production must
go slowly with a strict limit, and nobody should be able to ship a production rollout that
jumps to 50% at once. The policy becomes **Terraform**, at the root of the repository
(`versions.tf` is given; the `local` provider writes files, no cloud needed), and a deploy
workflow uses it.

**Variables** (`variables.tf`), each validated:

| Variable | Type, default | Refused when |
| --- | --- | --- |
| `service` | `string` | not a Kubernetes name, or longer than 56 characters (it gets `-stable` and `-canary`) |
| `namespace` | `string` | not a Kubernetes name |
| `environment` | `string` | not `staging` or `production` |
| `steps` | `list(object({ weight = number, pause = optional(string) }))` | the last weight is not 100; a weight is not an integer from 1 to 100; the weights do not go up; a pause is not a duration (`30s`, `5m`, `1h`); **in production**, fewer than 3 steps or a first weight above 10 |
| `interval` | `string`, `"1m"` | not a duration above zero |
| `max_error_ratio` | `number` | not strictly between 0 and 1 |
| `failure_limit` | `number`, `2` | not an integer of at least 1 |
| `query` | `string`, `null` | |
| `output_dir` | `string`, `"rollouts"` | |

**One resource**, a `local_file` named `rollout`, writes `OUTPUT_DIR/ENVIRONMENT/SERVICE.yaml`
(mode `0644`): the rollout spec of stage 1, as `yamlencode` of an object with `service`,
`namespace`, `route` (the service name), `stable` (`SERVICE-stable`), `canary` and
`deployment` (`SERVICE-canary`), the `steps` (a step without a pause may have `pause:
null`) and `analysis` with the `interval`, the `query` (when none is given, the 5xx ratio
of `http_requests_total` for `service="SERVICE-canary"` over the interval), `max`,
`failure_limit` and `inconclusive_limit: 3`. **Outputs** (`outputs.tf`): `file` (its
path), `steps` (how many) and `first_weight`.

**Two variable files**, `environments/staging.tfvars` and `environments/production.tfvars`,
both for `checkout` in `shop`:

| | interval | max_error_ratio | failure_limit | steps |
| --- | --- | --- | --- | --- |
| staging | `30s` | `0.02` | `1` | 50% for `2m`, then 100% |
| production | `1m` | `0.01` | `2` | 10% for `5m`, 25% for `5m`, 50% for `10m`, then 100% |

**`.github/workflows/deploy.yml`**, named `deploy`, started by `workflow_dispatch` with one
required `string` input, `version`. Read-only token, `defaults.run.shell: bash`, one
concurrency group with a fixed name (one deploy at a time) that is never cancelled in
progress. Jobs `staging`, then `production` (it needs `staging`), each in the environment
of its name, on `ubuntu-24.04` with a timeout. The steps of each job, in order:

1. `actions/checkout`, then `hashicorp/setup-terraform` with `terraform_version: 1.17.2`;
2. connect: the environment's secret `KUBECONFIG` (base64) reaches the step through `env:`;
   decode it to `$RUNNER_TEMP/kubeconfig` and put `KUBECONFIG=...` in `$GITHUB_ENV`, so the
   next steps' `kubectl` finds the cluster;
3. render: `terraform init -input=false`, then `terraform apply -input=false -auto-approve
   -var-file=environments/ENV.tfvars`;
4. start the canary: `kubectl -n shop set image deployment/checkout-canary
   app="ghcr.io/acme/checkout:$VERSION"`, with the version through `env:`;
5. roll out: `bash rollout.sh run rollouts/ENV/checkout.yaml`; a rollback fails the job,
   so production never starts after a bad staging;
6. a step that runs only when the run is cancelled (`if: ${{ cancelled() }}`) and aborts
   the rollout: `bash rollout.sh abort rollouts/ENV/checkout.yaml`. GitHub kills the
   rollout step on cancel; your `abort` from stage 4 then undoes what it did.

The hidden `terraform test` files plan the module with good and bad variables. The bats
tests apply both variable files and `plan` the result, then run each job's `run:` steps in a
copy of the repository, with the fakes and a kubeconfig that the fake `kubectl` checks.
