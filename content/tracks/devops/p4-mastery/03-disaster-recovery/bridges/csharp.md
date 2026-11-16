---
from: csharp
status: draft
concept: "Recovery is designed, tested and timed"
from_way: "SQL Server Always On availability groups and a DR site that is tested once a year, if at all."
native_way: "Recovery objectives per service, geo-replication or failover groups where needed, and regular measured failover drills."
sensei_tip: "Azure Chaos Studio can inject faults into App Service, AKS and VMs; start with a single-zone failure in staging."
---

| C# world | Disaster recovery |
| --- | --- |
| Always On availability groups | replication for RPO |
| a DR site | pilot light or warm standby |
| a yearly DR test | regular game days |
| Polly retries | surviving a failover |
