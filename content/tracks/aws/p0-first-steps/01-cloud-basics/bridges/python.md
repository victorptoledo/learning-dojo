---
from: python
status: draft
concept: "Everything in AWS is an API you call with signed credentials"
from_way: "A Django app runs on a VPS you patch yourself, with the database on the same box."
native_way: "boto3 calls AWS APIs with credentials from the provider chain; you choose how much to run yourself (EC2) or hand to AWS (RDS, Lambda)."
sensei_tip: "Create `boto3.Session(profile_name=...)` only on laptops; in AWS call `boto3.client(\"s3\")` with no keys and let the role supply them."
---

| Python world | AWS |
| --- | --- |
| `~/.ssh/config` hosts | `~/.aws/config` profiles and SSO sessions |
| `pip install` on the server | a managed runtime (Lambda) or your AMI |
| `os.environ["DB_PASSWORD"]` | a role and Secrets Manager |
| one VPS | one account, many regions and AZs |
