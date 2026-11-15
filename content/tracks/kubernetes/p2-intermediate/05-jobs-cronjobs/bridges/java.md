---
from: java
status: draft
concept: "Batch and scheduled work"
from_way: "Spring Batch jobs started by Quartz or @Scheduled inside the long-running web application."
native_way: "Batch work runs as its own Job or CronJob with its own resources, retries and logs, separate from the web Pods."
sensei_tip: "With @Scheduled inside a 3-replica Deployment the task runs three times; move it to a CronJob or guard it with a lock such as ShedLock."
---

| Java world | Kubernetes |
| --- | --- |
| `@Scheduled(cron = "0 0 2 * * *")` | a CronJob `schedule` |
| a Quartz misfire policy | `startingDeadlineSeconds` |
| Spring Batch retry | `backoffLimit` |
| a partitioned step | an Indexed Job |
