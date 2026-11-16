---
from: python
status: draft
concept: "Infrastructure has a build file and a dependency graph"
from_way: "A boto3 script creates resources step by step and breaks when run a second time."
native_way: "Terraform declares the resources; plan computes the difference with what exists, so a second apply changes nothing."
sensei_tip: "If you prefer Python to HCL, Pulumi and the CDK for Terraform exist, but the plan, state and module ideas are the same."
---

| Python world | Terraform |
| --- | --- |
| `boto3.client("s3").create_bucket` | `resource "aws_s3_bucket"` |
| function arguments with type hints | typed variables with validation |
| a package | a module |
| `pytest` | `terraform test` |
