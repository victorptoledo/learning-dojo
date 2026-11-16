---
from: java
status: draft
concept: "Objects, not files: whole objects in buckets, addressed by key"
from_way: "The app writes uploads to an NFS share and serves them through a servlet."
native_way: "Uploads go straight to S3 with presigned URLs; reads use S3 or CloudFront; lifecycle rules handle aging and deletion."
sensei_tip: "Use the S3 Transfer Manager (built on the CRT client) for large files; it does multipart uploads and parallel downloads for you."
---

| Java world | AWS |
| --- | --- |
| `Files.write(path, bytes)` | `s3.putObject(...)` |
| a servlet streaming downloads | a presigned GET URL or CloudFront |
| a nightly cleanup job | a lifecycle rule |
| `java.nio` directories | key prefixes |
