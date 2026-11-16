---
from: csharp
status: draft
concept: "Objects, not files: whole objects in buckets, addressed by key"
from_way: "The app saves files to a network share (`\\\\fileserver\\uploads`) and streams them from a controller."
native_way: "Files live in S3; clients upload with presigned URLs (`GetPreSignedURL`); lifecycle rules archive them."
sensei_tip: "Use `TransferUtility` from AWSSDK.S3 for big files; it handles multipart and parallel transfers."
---

| .NET world | AWS |
| --- | --- |
| `File.WriteAllBytesAsync` | `PutObjectAsync` |
| a UNC share | a bucket and key prefixes |
| `FileStreamResult` | a presigned GET URL |
| a cleanup scheduled task | lifecycle expiration |
