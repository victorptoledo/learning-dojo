---
from: python
status: draft
concept: "A private network you design: routes decide who can reach whom"
from_way: "The database listens on localhost and nginx is the only thing exposed."
native_way: "The database sits in a private subnet with no internet route, and only the app tier's security group may connect."
sensei_tip: "A Lambda function in a VPC loses internet access unless its subnets route to a NAT gateway; add VPC endpoints for the AWS services it calls instead."
---

| Python world | AWS |
| --- | --- |
| `bind 127.0.0.1` | a private subnet plus a security group |
| ufw rules | security groups (stateful) and NACLs (stateless) |
| `requests.get` to an external API | egress through a NAT gateway |
| `boto3` to S3 from a private subnet | a gateway VPC endpoint |
