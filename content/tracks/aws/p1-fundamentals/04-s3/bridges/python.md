---
from: python
status: draft
concept: "Objects, not files: whole objects in buckets, addressed by key"
from_way: "Django stores `FileField` uploads in `MEDIA_ROOT` on the server's disk."
native_way: "Files are objects in a private bucket; the browser uploads with a presigned URL; events trigger processing."
sensei_tip: "`django-storages` with the S3 backend keeps `FileField` working; generate presigned POSTs with `s3.generate_presigned_post` to limit size and content type."
---

| Python world | AWS |
| --- | --- |
| `MEDIA_ROOT` | a private S3 bucket |
| `open(path, "wb")` | `s3.put_object` or `upload_fileobj` |
| `os.listdir` | `list_objects_v2` with `Prefix` and a paginator |
| a Celery task on upload | an S3 event to SQS or Lambda |
