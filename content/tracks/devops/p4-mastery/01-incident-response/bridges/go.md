---
from: go
status: draft
concept: "Respond in roles, mitigate first, learn without blame"
from_way: "You SIGQUIT a stuck Go service to get its goroutine dump and restart it."
native_way: "The dump is evidence for later; the incident's first job is mitigation (roll back, fail over) coordinated by an incident commander."
sensei_tip: "Keep pprof reachable from inside the cluster so evidence can be taken during an incident without a redeploy."
---

| Go world | Incident response |
| --- | --- |
| SIGQUIT goroutine dump | evidence before restarting |
| `kubectl rollout undo` | a mitigation |
| `context` deadlines | shedding load |
| a commit message "fix prod" | a postmortem action item |
