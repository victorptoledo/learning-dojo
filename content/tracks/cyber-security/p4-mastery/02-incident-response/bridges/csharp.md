---
from: csharp
status: draft
concept: "Prepared, evidence-first, blameless"
from_way: "Reimaging a Windows server immediately and losing the event logs with it."
native_way: "Export event logs and a memory image, isolate the machine, investigate with the EDR, reset affected accounts and Kerberos secrets, then rebuild."
sensei_tip: "In Windows estates, a compromise often means credentials in memory: after containment, reset passwords of accounts that logged into the host and, for a domain compromise, the krbtgt account twice."
---

| C# | Incident-ready practice |
| --- | --- |
| reimage at once | export logs and memory first |
| reset one password | reset every account used on the host |
| Event Viewer by hand | central logs in the SIEM |
| ad hoc chat updates | a commander and a comms lead |
| no exercise | tabletops on a schedule |
