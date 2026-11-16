---
from: python
status: draft
concept: "Design for failure: spread, detect, replace, recover"
from_way: "One server and nightly `pg_dump` to another disk."
native_way: "Multi-AZ everything, AWS Backup with cross-account copies, and a DR strategy chosen from the RTO and RPO."
sensei_tip: "Set botocore's `Config(retries={\"mode\": \"adaptive\"}, connect_timeout=2, read_timeout=5)`: retries with backoff ride out short failures, and timeouts stop a dead dependency from blocking workers."
---

| Python world | AWS |
| --- | --- |
| nightly `pg_dump` | AWS Backup and PITR |
| one VPS | instances or tasks in several AZs |
| a cron health check | Route 53 and ALB health checks |
| tenacity retries | SDK retry modes with jitter |
