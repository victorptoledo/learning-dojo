---
from: go
status: draft
concept: "Design for failure: spread, detect, replace, recover"
from_way: "A Go binary on two hosts behind a hardware load balancer."
native_way: "Tasks across AZs behind ALBs or NLBs, Route 53 failover between regions, and data replicated to match the RPO."
sensei_tip: "Give every call a `context.WithTimeout` and use the SDK's `retry.AddWithMaxAttempts`; a Go service that never times out is not highly available."
---

| Go world | AWS |
| --- | --- |
| keepalived VIP | an NLB with static IPs per AZ |
| two hosts | an ASG or ECS service over three AZs |
| rsync to a DR site | S3 cross-region replication |
| a `/healthz` handler | target group health checks |
