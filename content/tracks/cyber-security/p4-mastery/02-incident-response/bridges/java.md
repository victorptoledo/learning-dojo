---
from: java
status: draft
concept: "Prepared, evidence-first, blameless"
from_way: "Restarting the JVM when something looks wrong, which clears the evidence along with the problem."
native_way: "Capture first (a heap dump with jcmd, thread dumps, logs), isolate the host, rotate the service's secrets, then redeploy from a known-good build."
sensei_tip: "`jcmd <pid> GC.heap_dump /path/file.hprof` and `jcmd <pid> Thread.print` capture the JVM's state before you restart; a heap dump may hold secrets, so store it like evidence."
---

| Java | Incident-ready practice |
| --- | --- |
| restart the service first | dump heap and threads, then isolate |
| patch the running server | redeploy a known-good build |
| rotate only the leaked password | rotate everything the service could reach |
| fix and forget | blameless review with actions |
| no runbooks | playbooks per incident type |
