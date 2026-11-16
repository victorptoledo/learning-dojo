---
from: python
status: draft
concept: "Declare the infrastructure and let the engine converge it"
from_way: "A Fabric or Ansible script creates resources step by step and breaks halfway through."
native_way: "A template declares the end state; CloudFormation orders, creates, rolls back and remembers it."
sensei_tip: "The CDK is natural in Python (`aws_cdk.aws_s3.Bucket(self, \"Reports\", versioned=True)`); run `cdk diff` in CI so reviewers see the change set."
---

| Python world | AWS |
| --- | --- |
| an imperative boto3 script | a declarative template |
| Ansible idempotence | stack updates and drift detection |
| `settings_prod.py` | stack parameters per environment |
| pytest on code | cfn-lint, cfn-guard and CDK assertions |
