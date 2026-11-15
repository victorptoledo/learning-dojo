---
from: javascript
status: draft
concept: "Finding why a service is slow"
from_way: "Node's --inspect with Chrome DevTools, clinic.js, and heap snapshots."
native_way: "The USE method on the machine, then the Node process with its own tools."
sensei_tip: "One core at 100% with the others idle means the event loop is blocked: Node runs your JavaScript on one thread. Use --cpu-prof or clinic flame, and cluster or workers to use more cores."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| node --inspect + DevTools | top, pidstat, then the profiler |
| heap snapshot | free -h, VmRSS in /proc/PID/status |
| FATAL ERROR: heap out of memory | --max-old-space-size versus the cgroup limit |
| EMFILE | ulimit -n, LimitNOFILE= |
