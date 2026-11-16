---
from: javascript
status: draft
concept: "Functions triggered by events, connected by managed services"
from_way: "An Express server stays up all day, with Bull queues on Redis for background jobs."
native_way: "Handlers are Lambda functions behind API Gateway, SQS and EventBridge; the platform scales and retries them."
sensei_tip: "Node.js is a fast cold-start runtime; bundle with esbuild (SAM or CDK do it) so only the code you import ships, and keep clients outside the handler."
---

| Node.js world | AWS |
| --- | --- |
| `app.post("/orders")` | an HttpApi event on a function |
| a Bull queue | SQS with a Lambda consumer |
| an EventEmitter | EventBridge rules |
| `node-cron` | EventBridge Scheduler |
