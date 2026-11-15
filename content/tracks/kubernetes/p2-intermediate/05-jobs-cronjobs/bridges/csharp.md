---
from: csharp
status: draft
concept: "Batch and scheduled work"
from_way: "Hangfire or Quartz.NET inside the web app, or a Windows Task Scheduler entry on a server."
native_way: "A CronJob schedules a console app container; each run is a Job with retries and a deadline."
sensei_tip: "Build the job as a small .NET console app (or a Worker that exits) and return a non-zero exit code on failure, so the Job retry logic works."
---

| .NET world | Kubernetes |
| --- | --- |
| Windows Task Scheduler | a CronJob |
| a Hangfire recurring job | a CronJob, or keep Hangfire with a database |
| a WebJob | a Job |
| an agent installed on every VM | a DaemonSet |
