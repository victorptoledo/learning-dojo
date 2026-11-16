---
from: javascript
status: draft
concept: "Design for failure: spread, detect, replace, recover"
from_way: "A Node.js app on one host behind a single nginx."
native_way: "Several tasks or functions across AZs behind an ALB or CloudFront, with Route 53 failover for regions."
sensei_tip: "Set `maxAttempts` and `requestHandler` timeouts on SDK v3 clients; the defaults can wait a long time on a broken network path."
---

| Node.js world | AWS |
| --- | --- |
| one nginx | an ALB across AZs |
| pm2 restart on crash | health checks that replace tasks |
| a CDN in front | CloudFront with origin failover |
| p-retry | SDK retries with backoff |
