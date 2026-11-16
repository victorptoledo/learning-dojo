---
from: javascript
status: draft
concept: "Scheduling and chaining jobs"
from_way: "node-cron or a CI schedule that runs scripts one after another."
native_way: "An orchestrated DAG with dependencies, retries and runs keyed by a logical date."
sensei_tip: "Promise.all is a dependency join and p-retry a retry policy, but neither keeps history across process restarts: that persistent state is what an orchestrator adds."
---

| JavaScript / TypeScript | Orchestration |
| --- | --- |
| node-cron schedule | time-based scheduling |
| await Promise.all([a, b]) then c | a fan-in dependency |
| p-retry | task retries |
| a date argument to the script | the logical date |
| GitHub Actions workflow needs: | a small DAG |
