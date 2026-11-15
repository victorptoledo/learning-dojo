---
from: go
status: draft
concept: "Batch and scheduled work"
from_way: "A goroutine with time.Ticker in the service, or robfig/cron inside the binary."
native_way: "The schedule moves out of the binary into a CronJob; the binary does one run and exits with a status code."
sensei_tip: "Read JOB_COMPLETION_INDEX in an Indexed Job to pick a shard: strconv.Atoi(os.Getenv(\"JOB_COMPLETION_INDEX\"))."
---

| Go world | Kubernetes |
| --- | --- |
| `cron.New().AddFunc(...)` | a CronJob |
| `os.Exit(1)` | a failed attempt |
| a worker pool over shards | an Indexed Job with `parallelism` |
| an agent per host | a DaemonSet |
