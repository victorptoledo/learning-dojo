---
from: java
status: draft
concept: "Respond in roles, mitigate first, learn without blame"
from_way: "A production bug means a developer gets a thread dump emailed to them and debugs it alone, under pressure."
native_way: "An incident commander coordinates, the operations lead rolls back first, and the thread dump is analysed calmly afterwards for the postmortem."
sensei_tip: "Capture jstack and jcmd GC.heap_info before restarting a JVM: evidence for the postmortem, taken in seconds."
---

| Java world | Incident response |
| --- | --- |
| a bug ticket marked urgent | a declared incident with a severity |
| an emailed stack trace | evidence attached to the timeline |
| a hotfix release | roll back first, fix forward later |
| a lessons-learned email | a blameless postmortem |
