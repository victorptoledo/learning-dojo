---
from: go
status: draft
concept: "Scheduling and chaining jobs"
from_way: "A long-running binary with time.Ticker loops, or several cron entries."
native_way: "An orchestrator running a DAG of idempotent tasks, each for a logical date."
sensei_tip: "errgroup.Group is a fan-out and fan-in of tasks within one process; Temporal (written in Go) is a durable workflow engine when you need state that survives restarts."
---

| Go | Orchestration |
| --- | --- |
| time.Ticker loop | time-based scheduling |
| errgroup.Wait() | waiting for upstream tasks |
| a retry loop with backoff | task retries |
| a -date flag | the logical date |
| Temporal workflows | durable, resumable orchestration |
