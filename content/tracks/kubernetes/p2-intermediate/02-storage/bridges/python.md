---
from: python
status: draft
concept: "Storage that outlives the process"
from_way: "Uploaded files go to MEDIA_ROOT on the server's disk, which breaks as soon as there are two servers."
native_way: "Two replicas cannot share a ReadWriteOnce disk; use object storage for uploads, and PVCs (with StatefulSets) for things that must own a disk."
sensei_tip: "django-storages or boto3 with S3 keeps Django media out of the Pod entirely; that is what lets the web tier scale freely."
---

| Python world | Kubernetes |
| --- | --- |
| `MEDIA_ROOT` on local disk | object storage, or a ReadWriteMany volume |
| a SQLite file | one Pod with a PVC (or, better, a real database) |
| `tempfile.mkdtemp()` | an `emptyDir` |
| a Redis server with AOF | a StatefulSet with a PVC per replica |
