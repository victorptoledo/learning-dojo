---
from: javascript
status: draft
concept: "Objects, not files: whole objects in buckets, addressed by key"
from_way: "Express uses multer to write uploads to the server disk, then streams them back."
native_way: "The front end uploads directly with a presigned URL from `@aws-sdk/s3-request-presigner`; nothing passes through Node.js."
sensei_tip: "Use `@aws-sdk/lib-storage`'s `Upload` for streams of unknown size; it splits them into multipart parts and retries each part."
---

| Node.js world | AWS |
| --- | --- |
| multer to disk | a presigned PUT URL |
| `fs.createReadStream` | `GetObjectCommand` streaming the body |
| a public `/uploads` folder | a private bucket behind CloudFront |
| chokidar watching a folder | S3 event notifications |
