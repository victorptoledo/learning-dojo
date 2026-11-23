# Sidequest: Canary controller

`checkout` is released with a canary: a second Deployment runs the new version, and a
Gateway API HTTPRoute splits the traffic between the `checkout-stable` and
`checkout-canary` Services by weight. Today someone edits the weights by hand and watches a
dashboard. You write the controller that does it, a small cousin of Argo Rollouts and
Flagger, in five stages:

1. `validate-plan`: `rollout.sh validate` and `plan` read `rollout.yaml` and refuse a bad
   spec before it touches the cluster;
2. `shift-traffic`: `rollout.sh run` moves the weights of the HTTPRoute step by step,
   pausing between steps;
3. `analysis`: during every pause it measures the canary with a Prometheus query and rolls
   back by itself when the canary is bad, or when nobody can tell;
4. `state-and-abort`: it records where it is, resumes after being killed, refuses a second
   rollout of the same service, and can be aborted;
5. `infrastructure`: the rollout policy of each environment comes from a Terraform module
   with validated variables, and a deploy workflow runs the controller and aborts it when
   the run is cancelled.

How it is checked:

- `rollout.sh` is bash, with `yq`, `jq` and `flock` available. The tests run it for real,
  with **fakes first in `PATH`**: a `kubectl` that keeps an HTTPRoute and a Deployment as
  JSON, applies your patches and records every call; a `promq` (the query command of this
  dojo: `promq QUERY` prints one number) that answers from a script the test writes; and a
  `sleep` that returns at once and records how long you asked for (or holds, when a test
  needs to catch the controller mid-rollout).
- They read what the cluster looks like afterwards, the order of the weights, the exit
  status and what `rollout.sh` prints. A rollout that ends with the canary at 0% and its
  Deployment scaled to 0 is a rollback; one that ends at 100% is a promotion.
- Stage 5 runs `terraform test` on hidden test files and `terraform apply` with the local
  provider (no network), and runs the deploy workflow's `run:` steps like CI would.
- Every `.sh` file must pass ShellCheck, the workflow `actionlint`, the Terraform files
  `terraform fmt` and `validate`. The tests run in Debian 13 without network.

Useful reading: the Gateway API docs on HTTPRoute traffic splitting, the Argo Rollouts docs
on canary steps and analysis, `man flock`, and the Terraform docs on input validation and
`terraform test`. When you are done, `[A]` copies a review prompt: ask an AI about your
controller.
