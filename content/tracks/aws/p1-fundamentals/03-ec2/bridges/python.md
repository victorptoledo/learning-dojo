---
from: python
status: draft
concept: "Instances are disposable; build them from images and give them roles"
from_way: "A Python app is deployed with Fabric to two servers set up by hand."
native_way: "A launch template with an AMI and user data builds identical instances; boto3 on the instance gets its role from IMDSv2."
sensei_tip: "Use `boto3` on EC2 with no keys at all: the credential chain asks IMDSv2 for the instance profile's credentials and refreshes them before they expire."
---

| Python world | AWS |
| --- | --- |
| `fab deploy` | bake an AMI, or user data that pulls the artifact |
| supervisord | systemd units started by user data |
| a cron box | EventBridge Scheduler or a small ASG |
| `~/.aws/credentials` on the server | an instance profile |
