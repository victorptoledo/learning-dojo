"""Publishes a folder to the site bucket: only what changed, with the right headers."""


def publish(s3, bucket: str, folder, dry_run: bool = False) -> dict[str, list[str]]:
    """Makes the bucket hold exactly the files of `folder`, and says what it did.

    `s3` is a boto3 S3 client. Returns
    {"uploaded": [...], "unchanged": [...], "deleted": [...], "invalidate": [...]},
    each list sorted.
    """
    raise NotImplementedError("todo: publish the folder")
