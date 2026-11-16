---
from: csharp
status: draft
concept: "Scheduling and chaining jobs"
from_way: "SQL Server Agent jobs or Hangfire recurring jobs with continuations."
native_way: "DAGs with dependencies, retries, logical dates and backfills, in Azure Data Factory or Airflow."
sensei_tip: "Hangfire continuations (ContinueJobWith) build chains, and Azure Data Factory pipelines are DAGs of activities with retry policies and tumbling-window triggers for backfills."
---

| C# | Orchestration |
| --- | --- |
| SQL Server Agent schedule | time-based scheduling |
| Hangfire ContinueJobWith | a dependency |
| Polly retry policy | task retries |
| ADF tumbling window trigger | logical dates and backfills |
| ADF activity dependency conditions | upstream failed handling |
