The infrastructure is ready; now the files. Write `publish(s3, bucket, folder, dry_run=False)`
in `publish.py`. `s3` is a boto3 S3 client, `folder` a path. After a call the bucket holds
exactly the files of the folder, and the function returns what it did:

```python
{"uploaded": ["about.html", "assets/app.3f9c.css"],   # sent with put_object
 "unchanged": ["index.html"],                          # already there, same content
 "deleted": ["old.html"],                              # in the bucket, gone from the folder
 "invalidate": ["/about.html", "/old.html"]}           # paths CloudFront must forget
```

Every list is sorted. The rules:

| Rule | Detail |
| --- | --- |
| keys | the path relative to `folder`, with `/` (`assets/logo.svg`); files and folders whose name starts with `.` are skipped |
| `ContentType` | `.html` `text/html; charset=utf-8`, `.css` `text/css; charset=utf-8`, `.js` `text/javascript; charset=utf-8`, `.json` `application/json`, `.txt` `text/plain; charset=utf-8`, `.svg` `image/svg+xml`, `.png` `image/png`, `.jpg`/`.jpeg` `image/jpeg`, `.webp` `image/webp`, `.ico` `image/x-icon`, `.woff2` `font/woff2`, anything else `application/octet-stream` (extensions in any case) |
| `CacheControl` | pages (`.html`) `no-cache`; anything under `assets/` (names carry a content hash) `public, max-age=31536000, immutable`; the rest `public, max-age=3600` |
| unchanged | a file whose MD5 equals the object's `ETag` is not sent again. Read the ETags from **one listing** of the bucket (`list_objects_v2`, every page): no `head_object` per file |
| order | pages are uploaded **after** every other file, so a new page never links to an asset that is not there yet; deletions come last |
| deletions | `delete_objects`, at most 1,000 keys per call (the S3 limit); raise an error if the answer lists `Errors` |
| invalidate | `/` + key for every key that **already existed** and was overwritten or deleted, plus `/` when `index.html` is one of them; new keys need no invalidation |
| dry run | `dry_run=True` returns the same answer and changes nothing: no `put_object`, no `delete_objects` |

The tests run your function against moto's in-memory S3 and record every request it sends,
so they see the order, the headers, the batch sizes and the listings.

> [!TIP]
> For objects uploaded in one `put_object` (not multipart), S3's `ETag` is the MD5 of the
> body in hex, between double quotes: `'"5d41402abc4b2a76b9719d911017c592"'`.
> `hashlib.md5(data, usedforsecurity=False).hexdigest()` gives the same text without them.
