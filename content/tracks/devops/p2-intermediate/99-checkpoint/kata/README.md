# Checkpoint 2: Infrastructure as Code with a Plan/Apply Pipeline

The platform's Terraform started with one hand-written module call (`module "web"` in
`main.tf`) and is already applied. Turn it into a configuration that scales to many
services, and write the pipeline that plans every change in its pull request and applies it
after the merge.

## Terraform (the root module is the kata folder)

`versions.tf`, `backend.hcl`, `prod.tfvars` and `modules/service/` are done. Write:

| File | Rule |
| --- | --- |
| `variables.tf` | `services`: a `map(object({ replicas = optional(number, 2), public = optional(bool, false) }))`, rejected when empty or when a name does not match `^[a-z][a-z0-9-]{2,20}$` |
| `main.tf` | one `module "service"` per entry of `var.services` (`for_each`), passing `name` (the key), `environment`, `replicas` and `public`; remove `module "web"` |
| `main.tf` | a `moved` block from `module.web` to `module.service["web"]`, so the applied web service is renamed in the state instead of destroyed and recreated |
| `outputs.tf` | `endpoints`: a map from each service name to its module's `endpoint`; `public_services`: the names of the public services, sorted |

## The pipeline (`.github/workflows/terraform.yml`)

| Rule | How |
| --- | --- |
| runs on pull requests and on pushes to `main` that change `**.tf` or the workflow itself | `paths: ["**.tf", ".github/workflows/terraform.yml"]` on both |
| the token is read-only by default; every job has a timeout; every action is pinned to a SHA | as in checkpoint 1 |
| only one run touches the state at a time, and a running apply is never cancelled | `concurrency: { group: terraform, cancel-in-progress: false }` |
| job `plan` runs only for pull requests: checkout, Terraform `1.17.2`, AWS credentials, then `terraform fmt -check -recursive`, `terraform init -input=false -backend-config=backend.hcl`, `terraform validate`, `terraform plan -input=false -var-file=prod.tfvars -out=tfplan`, and uploads `tfplan` as an artifact | `if: github.event_name == 'pull_request'` |
| job `apply` runs only for pushes to `main`, in the `production` environment (where reviewers approve): checkout, Terraform, AWS credentials, the same `init`, then `terraform apply -input=false -auto-approve -var-file=prod.tfvars` | `if:` on the event and `github.ref`, `environment: production` |
| AWS credentials come from **OIDC**, never from stored keys: each job has `id-token: write` (and `contents: read`) and assumes its own role, `arn:aws:iam::123456789012:role/terraform-plan` or `.../terraform-apply`, in `eu-west-1` | `aws-actions/configure-aws-credentials` with `role-to-assume` |

Pins you can use:

| Action | Commit | Version |
| --- | --- | --- |
| `actions/checkout` | `3d3c42e5aac5ba805825da76410c181273ba90b1` | v7.0.1 |
| `hashicorp/setup-terraform` | `dfe3c3f87815947d99a8997f908cb6525fc44e9e` | v4.0.1 |
| `aws-actions/configure-aws-credentials` | `e1253824e5c10ff9df46874f81ed3ec929e19cfd` | v6.3.0 |
| `actions/upload-artifact` | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` | v7.0.2 |

The dojo runs `terraform fmt`, `validate` and hidden `terraform test` files offline (the S3
backend is not used: tests keep their state in memory), checks the workflow with actionlint
and reads both with bats.

There are 23 tests: 1 in `tests/visible.bats`, 11 hidden bats tests, actionlint, `fmt`,
`validate` for the root and the module, and 7 `terraform test` runs.
