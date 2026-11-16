---
from: javascript
status: draft
concept: "A task definition is the Pod; a service keeps it running"
from_way: "A Node.js image runs on a VM with pm2 or compose and a reverse proxy in front."
native_way: "The image runs as ECS tasks behind an ALB, with health checks, rolling deployments and auto scaling."
sensei_tip: "Handle SIGTERM (`server.close()`) so ECS drains connections; set the ALB deregistration delay to match how long requests take."
---

| Node.js world | AWS |
| --- | --- |
| nginx in front | an Application Load Balancer |
| pm2 cluster mode | several ECS tasks |
| `docker logs` | CloudWatch Logs via awslogs |
| a Heroku dyno | a Fargate task |
