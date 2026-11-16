---
from: java
status: draft
concept: "Pipelines with validation between stages"
from_way: "A service method that calls a chain of collaborators, or a Stream pipeline of map and filter stages."
native_way: "A chain of prompts where each step has one job, a typed output, and a check in code before the next step runs."
sensei_tip: "Design a chain like a batch job with Spring Batch steps: each step reads, processes and writes a known format, failures are retried a bounded number of times, and every execution is logged."
---

| Java | Chaining concept |
| --- | --- |
| a `Stream` of `map` stages | a sequential chain |
| `filter` or a validator between stages | a gate |
| a `switch` that picks a strategy | routing |
| `CompletableFuture.allOf(...)` | parallel sections |
| Spring Retry with `maxAttempts` | bounded retries per step |
