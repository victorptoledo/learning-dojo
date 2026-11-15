---
from: csharp
status: draft
concept: "Storage that outlives the process"
from_way: "Files written to a share on a Windows server, or to Azure Files mounted on an App Service."
native_way: "A PVC of an Azure Files class (ReadWriteMany) or a managed disk class (ReadWriteOnce) gives Pods persistent storage; the StorageClass decides which."
sensei_tip: "Store ASP.NET Core Data Protection keys outside the Pod (blob storage or a database) or every new Pod invalidates the cookies issued by the old ones."
---

| .NET world | Kubernetes |
| --- | --- |
| a UNC file share | a ReadWriteMany PVC (Azure Files) |
| a managed disk on a VM | a ReadWriteOnce PVC (Azure Disk CSI) |
| SQL Server Always On nodes | a StatefulSet (or Azure SQL) |
| `Path.GetTempPath()` | an `emptyDir` |
