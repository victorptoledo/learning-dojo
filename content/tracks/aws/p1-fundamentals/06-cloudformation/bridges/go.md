---
from: go
status: draft
concept: "Declare the infrastructure and let the engine converge it"
from_way: "A shell script with `aws` CLI calls creates resources; running it twice fails."
native_way: "Templates declare the end state; stacks update idempotently and roll back on failure."
sensei_tip: "The CDK works in Go (`awscdk.NewStack`, `awss3.NewBucket`), though pointers like `jsii.String(\"x\")` make it verbose; many Go teams keep YAML templates or Terraform."
---

| Go world | AWS |
| --- | --- |
| a bash script of CLI calls | a template deployed as a stack |
| `go generate` | `cdk synth` writing CloudFormation |
| build tags per environment | conditions and parameters |
| `go vet` | cfn-lint and cfn-guard in CI |
