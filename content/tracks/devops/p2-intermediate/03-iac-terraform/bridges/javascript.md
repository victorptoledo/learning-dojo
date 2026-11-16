---
from: javascript
status: draft
concept: "Infrastructure has a build file and a dependency graph"
from_way: "You click through a cloud console or a hosting dashboard to set up domains, buckets and functions."
native_way: "The same resources are declared in .tf files, reviewed in pull requests and applied by a pipeline."
sensei_tip: "The AWS CDK and CDKTF let you write infrastructure in TypeScript; they compile to the same kind of plan."
---

| JavaScript world | Terraform |
| --- | --- |
| `package.json` dependencies | `required_providers` |
| `package-lock.json` | `.terraform.lock.hcl` |
| an npm package | a module |
| `JSON.stringify` | `jsonencode()` |
