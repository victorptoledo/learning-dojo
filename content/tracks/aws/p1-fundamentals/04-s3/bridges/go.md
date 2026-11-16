---
from: go
status: draft
concept: "Objects, not files: whole objects in buckets, addressed by key"
from_way: "A Go service writes files to a mounted volume and serves them with `http.FileServer`."
native_way: "Objects go to S3 through the SDK or presigned URLs, and are served by S3 or CloudFront."
sensei_tip: "Use `s3.NewPresignClient(client).PresignPutObject` for uploads and the `feature/s3/manager` uploader for multipart streams."
---

| Go world | AWS |
| --- | --- |
| `os.WriteFile` | `PutObject` |
| `http.FileServer` | CloudFront in front of a private bucket |
| `filepath.Walk` | `ListObjectsV2` paginator |
| fsnotify | S3 event notifications |
