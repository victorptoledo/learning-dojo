---
from: javascript
status: draft
concept: "Processes and stopping cleanly"
from_way: "child_process.spawn, one event loop per Node process, and process.on('SIGTERM', ...)."
native_way: "Processes with PIDs, signals between them, and supervisors that send TERM and then KILL."
sensei_tip: "Node exits on SIGTERM unless you listen for it. In a container, PID 1 gets no default signal handling, so run node with --init (tini) or handle SIGTERM yourself and close the server."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| child_process.spawn('ls') | fork + exec |
| process.pid | $$ |
| process.on('SIGTERM', stop) | trap stop TERM |
| child.kill('SIGTERM') | kill PID |
| process.exitCode = 0 | exit 0 after cleanup |
