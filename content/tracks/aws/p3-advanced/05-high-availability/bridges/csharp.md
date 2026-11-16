---
from: csharp
status: draft
concept: "Design for failure: spread, detect, replace, recover"
from_way: "Availability sets and SQL Server Always On in one datacentre, with Azure Site Recovery for DR."
native_way: "Multi-AZ deployments in a region, Aurora or RDS Multi-AZ, and pilot light or warm standby in a second region."
sensei_tip: "Polly policies (retry with jitter, timeout, circuit breaker) still apply; combine them with health checks the ALB uses, so bad instances leave the pool quickly."
---

| .NET world | AWS |
| --- | --- |
| availability zones in Azure | Availability Zones in AWS |
| Traffic Manager | Route 53 routing policies |
| Azure Front Door | CloudFront and Global Accelerator |
| Site Recovery | AWS Elastic Disaster Recovery or pilot light |
