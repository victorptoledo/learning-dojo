"""Publishes a folder to the site bucket: only what changed, with the right headers."""

import hashlib
from pathlib import Path, PurePosixPath

CONTENT_TYPES = {
    ".html": "text/html; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".json": "application/json",
    ".txt": "text/plain; charset=utf-8",
    ".svg": "image/svg+xml",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".webp": "image/webp",
    ".ico": "image/x-icon",
    ".woff2": "font/woff2",
}
DELETE_BATCH = 1000


def content_type(key: str) -> str:
    return CONTENT_TYPES.get(PurePosixPath(key).suffix.lower(), "application/octet-stream")


def cache_control(key: str) -> str:
    if key.endswith(".html"):
        return "no-cache"
    if key.startswith("assets/"):
        return "public, max-age=31536000, immutable"
    return "public, max-age=3600"


def local_files(folder: Path) -> dict[str, Path]:
    files = {}
    for path in sorted(folder.rglob("*")):
        relative = path.relative_to(folder)
        if path.is_file() and not any(part.startswith(".") for part in relative.parts):
            files[relative.as_posix()] = path
    return files


def remote_etags(s3, bucket: str) -> dict[str, str]:
    etags = {}
    for page in s3.get_paginator("list_objects_v2").paginate(Bucket=bucket):
        for item in page.get("Contents", []):
            etags[item["Key"]] = item["ETag"].strip('"')
    return etags


def publish(s3, bucket: str, folder, dry_run: bool = False) -> dict[str, list[str]]:
    files = local_files(Path(folder))
    remote = remote_etags(s3, bucket)
    uploads, unchanged = [], []
    for key, path in files.items():
        digest = hashlib.md5(path.read_bytes(), usedforsecurity=False).hexdigest()
        (unchanged if remote.get(key) == digest else uploads).append(key)
    deleted = sorted(set(remote) - set(files))

    if not dry_run:
        # Pages last: a new page never points at an asset that is not there yet.
        for key in sorted(uploads, key=lambda k: (k.endswith(".html"), k)):
            s3.put_object(Bucket=bucket, Key=key, Body=files[key].read_bytes(),
                          ContentType=content_type(key), CacheControl=cache_control(key))
        for start in range(0, len(deleted), DELETE_BATCH):
            batch = deleted[start:start + DELETE_BATCH]
            answer = s3.delete_objects(Bucket=bucket, Delete={
                "Objects": [{"Key": key} for key in batch], "Quiet": True})
            if answer.get("Errors"):
                raise RuntimeError(f"could not delete {[e['Key'] for e in answer['Errors']]}")

    replaced = [key for key in uploads if key in remote] + deleted
    invalidate = {"/" + key for key in replaced}
    if "index.html" in replaced:
        invalidate.add("/")
    return {"uploaded": sorted(uploads), "unchanged": sorted(unchanged),
            "deleted": deleted, "invalidate": sorted(invalidate)}
