# Infrastructure as Code: Terraform Modules, State and Plans

Networks, clusters, databases, DNS records and IAM roles used to be clicked together in a
console. Nobody could review the change, repeat it in another region or say how production
differed from staging. **Infrastructure as code** (IaC) describes infrastructure in files that
are versioned, reviewed and applied by a tool. **Terraform** (and its open source fork
**OpenTofu**, which reads the same language) is the most widely used one; CloudFormation,
Bicep, Pulumi and Crossplane follow the same ideas.

## The language

Terraform files (`.tf`) use HCL. A minimal AWS example:

```hcl
terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

provider "aws" {
  region = var.region
}

variable "region" {
  type    = string
  default = "eu-west-1"
}

variable "buckets" {
  type = map(object({ versioned = optional(bool, true) }))
}

locals {
  tags = { managed_by = "terraform", team = "platform" }
}

resource "aws_s3_bucket" "this" {
  for_each = var.buckets
  bucket   = "acme-${each.key}"
  tags     = local.tags
}

data "aws_caller_identity" "current" {}

output "bucket_arns" {
  value = { for name, b in aws_s3_bucket.this : name => b.arn }
}
```

| Block | Role |
| --- | --- |
| `provider` | a plugin that talks to an API (AWS, Azure, Google, Kubernetes, GitHub, Datadog...) |
| `resource` | something Terraform creates and manages |
| `data` | something Terraform only reads |
| `variable`, `locals`, `output` | inputs (with types and `validation`), computed values, results |
| `for_each`, `count` | many instances from a map or a number; prefer `for_each`, whose keys are stable |
| `module` | a reusable package of resources with its own variables and outputs |

References between resources (`aws_s3_bucket.this["logs"].arn`) form a dependency graph;
Terraform creates and destroys things in the right order and in parallel.

## The workflow

```bash
terraform init       # download providers and modules, set up the backend
terraform fmt        # canonical formatting
terraform validate   # syntax and types
terraform plan -out=tfplan
terraform apply tfplan
```

`plan` compares three things: the **configuration** (what you want), the **state** (what
Terraform created) and the **real infrastructure** (refreshed from the APIs), and prints what
it will create (`+`), change (`~`), replace (`-/+`) or destroy (`-`). Read every plan: a
`-/+` on a database means data loss. `plan -detailed-exitcode` returns 2 when there are
changes, which makes a scheduled drift check easy.

## State

The **state** file maps each resource address to a real object ID. It is the most sensitive
file in IaC:

- keep it in a **remote backend** (S3 with native locking, Azure Storage, GCS, HCP Terraform),
  never in Git; it can contain secrets in plain text, so encrypt it and restrict access;
- **locking** stops two applies from corrupting it;
- split it: one state per environment and per large component (network, cluster, apps), so a
  plan is small and a mistake has a small **blast radius**.

Refactoring without destroying: a **`moved` block** tells Terraform that an address changed
(`module.web` → `module.service["web"]`); an **`import` block** brings an existing object
under management; `removed` stops managing something without deleting it. Avoid hand edits
with `terraform state`; use these blocks so the change is reviewed like code.

## Modules and environments

A **module** is a folder of `.tf` files; the folder you run Terraform in is the **root module**.

```text
modules/
  network/       variables.tf main.tf outputs.tf
  service/
envs/
  dev/           main.tf (calls modules), backend.tf, dev.tfvars
  prod/
```

Good modules have a small, typed interface with validations, sensible defaults, outputs for
what callers need, and pinned provider requirements. Pin module versions too (a Git tag or a
registry version). **Workspaces** can hold several states for one configuration, but separate
root folders per environment are clearer for production.

## Running Terraform in a team

- Format, validate, lint (`tflint`) and scan (`checkov`, `trivy config`) in CI.
- **Plan on every pull request** and show the plan to reviewers.
- **Apply from the pipeline** after merge, with the reviewed plan or a fresh plan, behind an
  environment approval for production; never apply from laptops.
- Give the pipeline short-lived cloud credentials through **OIDC**, with separate roles for
  plan (read-only) and apply.
- Commit `.terraform.lock.hcl` so every run uses the same provider builds.
- Test modules with `terraform test` (`.tftest.hcl` files that plan or apply and assert).

## Summary

- IaC makes infrastructure reviewable, repeatable and auditable.
- Terraform: providers, resources, data sources, variables, locals, outputs, `for_each`,
  modules; references build the dependency graph.
- `plan` shows the diff between configuration, state and reality; read it before `apply`.
- State is sensitive: remote, locked, encrypted, split by blast radius; refactor with `moved`
  and `import` blocks.
- Teams plan in pull requests and apply from pipelines with OIDC credentials.

## Further reading

- Terraform language documentation: <https://developer.hashicorp.com/terraform/language>
- Terraform tests: <https://developer.hashicorp.com/terraform/language/tests>
- Refactoring with moved blocks: <https://developer.hashicorp.com/terraform/language/modules/develop/refactoring>
- OpenTofu documentation: <https://opentofu.org/docs/>
- *Terraform: Up & Running*, 3rd edition, Yevgeniy Brikman (O'Reilly, 2022)
