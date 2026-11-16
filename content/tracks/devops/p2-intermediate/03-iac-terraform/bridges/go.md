---
from: go
status: draft
concept: "Infrastructure has a build file and a dependency graph"
from_way: "You call cloud SDKs from Go and keep track of created IDs yourself."
native_way: "Terraform (written in Go) keeps the IDs in state, computes the diff in plan and calls providers, which are Go plugins."
sensei_tip: "A custom provider is a Go program built with the terraform-plugin-framework; worth knowing when an internal API has none."
---

| Go world | Terraform |
| --- | --- |
| a package | a module |
| `go.mod` | `required_providers` |
| `go.sum` | `.terraform.lock.hcl` |
| `go vet` | `terraform validate` |
