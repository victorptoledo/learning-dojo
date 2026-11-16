---
from: java
status: draft
concept: "Scheduling and chaining jobs"
from_way: "@Scheduled methods or Quartz triggers, one per job, with ordering by convention."
native_way: "A DAG of tasks with explicit dependencies, retries, logical dates and backfills, run by an orchestrator."
sensei_tip: "Spring Batch's step flow is a small DAG inside one job; an orchestrator manages DAGs across systems and adds history, backfills and a UI."
---

| Java | Orchestration |
| --- | --- |
| @Scheduled(cron = ...) | time-based scheduling |
| Spring Batch Job of Steps | a DAG inside one job |
| @Retryable(maxAttempts = 3) | task retries |
| JobParameters with a date | the logical date |
| CompletableFuture.allOf | waiting for upstream tasks |
