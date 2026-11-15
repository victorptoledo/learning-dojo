---
from: javascript
status: draft
concept: "Batch and scheduled work"
from_way: "node-cron inside the server process, or a scheduled serverless function."
native_way: "Scheduled work runs as a CronJob that starts a short-lived container, separate from the web replicas."
sensei_tip: "node-cron in a Deployment fires once per replica; a CronJob runs it once, and its exit code tells Kubernetes whether to retry."
---

| Node.js world | Kubernetes |
| --- | --- |
| `cron.schedule('0 2 * * *', fn)` | a CronJob |
| a scheduled cloud function | a CronJob with `timeZone` |
| `process.exit(1)` on failure | a failed attempt counted by `backoffLimit` |
| a queue consumer that drains and exits | a Job (or KEDA ScaledJob) |
