---
from: java
status: draft
concept: "Storage that outlives the process"
from_way: "A JNDI DataSource names a database the operations team provides; the app never knows which disk holds the data."
native_way: "A PVC is the same kind of indirection for files: the app asks for a class and a size, and the cluster provides a volume."
sensei_tip: "Most Java services should stay stateless and keep data in a database or object storage; reach for a PVC only for things like an embedded search index or a cache warm-up file."
---

| Java world | Kubernetes |
| --- | --- |
| a JNDI resource name | a PersistentVolumeClaim name |
| the DBA's storage choice | a StorageClass |
| a clustered Kafka or Elasticsearch | a StatefulSet (often run by an operator) |
| `java.io.tmpdir` | an `emptyDir` |
