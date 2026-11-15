---
from: javascript
status: draft
concept: "Storage that outlives the process"
from_way: "multer writes uploads to ./uploads, which lives only on that one server."
native_way: "Container file systems are thrown away; persistent files need a PVC, and shared ones need object storage or a ReadWriteMany class."
sensei_tip: "Write uploads to S3-compatible storage with pre-signed URLs; your Node.js Pods then stay stateless and can be replaced at any time."
---

| Node.js world | Kubernetes |
| --- | --- |
| `./uploads` folder | object storage, not the container |
| `os.tmpdir()` | an `emptyDir` |
| a MongoDB replica set | a StatefulSet (or an operator, or a managed service) |
| a Docker named volume | a PersistentVolumeClaim |
