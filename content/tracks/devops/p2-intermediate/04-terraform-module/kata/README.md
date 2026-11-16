# A Terraform Module with Guard Rails

Write a reusable Terraform module that describes a service's deployment: its name, standard
tags and one configuration file per region. Real modules would create cloud resources; this
one writes files with the `hashicorp/local` provider so it runs anywhere, but the language
is the same: variables with validation, locals, `for_each` and outputs.

**`versions.tf`**: Terraform `>= 1.9`, and `hashicorp/local` with version `~> 2.9`.

**`variables.tf`**:

| Variable | Type | Default | Rejected when |
| --- | --- | --- | --- |
| `service` | `string` | none | it does not match `^[a-z][a-z0-9-]{2,20}$` |
| `environment` | `string` | none | it is not `dev`, `staging` or `prod` |
| `replicas` | `number` | `2` | it is not a whole number from 1 to 10, or it is below 3 in `prod` |
| `regions` | `set(string)` | `["eu-west-1"]` | it is empty |
| `extra_tags` | `map(string)` | `{}` | never |

**`main.tf`**:

- `local.name` is `<service>-<environment>`, for example `checkout-dev`;
- `local.tags` is `extra_tags` plus `service`, `environment` and `managed_by = "terraform"`;
  the standard tags win if `extra_tags` has the same keys;
- one `local_file` named `config` **per region** (`for_each`), at
  `out/<region>/<name>.json`, `file_permission = "0644"`, whose content is the JSON
  object `{ name, region, replicas, tags }` (`jsonencode`).

**`outputs.tf`**: `name`, `tags`, and `config_files`, a map from each region to its file name.

The dojo runs `terraform fmt -check`, `terraform init` and `terraform validate` (offline,
with the providers baked into its image), then hidden `terraform test` files: each `run`
block plans the module with some variables and checks the result, or expects a validation
to fail. Try it yourself with `terraform init` and `terraform plan -var service=checkout
-var environment=dev`.

There are 15 tests: 1 in `tests/visible.bats`, 2 hidden bats tests, `fmt`, `validate` and
10 `terraform test` runs.
