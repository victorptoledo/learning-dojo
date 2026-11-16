# S3: Buckets, Storage Classes, Encryption and Access

**Amazon S3** (Simple Storage Service) stores **objects**: any bytes up to 5 TB, addressed
by a **bucket** and a **key**. It is designed for eleven nines of durability, scales without
limits you will hit, and sits behind data lakes, backups, static sites, logs and uploads.

## Buckets, keys and prefixes

A bucket name is **globally unique** across all AWS accounts and lives in one region.
Inside it there are no folders: `invoices/2026/0042.pdf` is one key, and `invoices/2026/`
is a **prefix** that the console shows as a folder. Prefixes matter for listing
(`ListObjectsV2` with `Prefix` and `Delimiter`), for IAM (`s3:prefix`) and for throughput
(each prefix supports at least 3,500 writes and 5,500 reads per second).

S3 is **strongly consistent**: after a successful `PUT`, every `GET` and `LIST` sees the new
object. Objects are written whole; there is no append or in-place edit. Uploads over
100 MB should use **multipart upload** (the SDKs' transfer managers do it for you).

## Storage classes and lifecycle

| Class | For | Notes |
| --- | --- | --- |
| Standard | hot data | the default |
| Intelligent-Tiering | unknown or changing access | moves objects between tiers by itself for a small monitoring fee |
| Standard-IA, One Zone-IA | infrequent access, read in milliseconds | per-GB retrieval fee, 30-day minimum; One Zone keeps one AZ |
| Glacier Instant Retrieval | archives read a few times a year, in milliseconds | 90-day minimum |
| Glacier Flexible Retrieval, Deep Archive | archives, restored in minutes to hours | cheapest; 90 or 180-day minimum |
| Express One Zone | single-digit-millisecond access, one AZ | directory buckets, for ML and analytics |

**Lifecycle rules** move objects between classes and delete them by age, prefix or tag:

```yaml
LifecycleConfiguration:
  Rules:
    - Id: uploads-cool-down
      Status: Enabled
      Prefix: uploads/
      Transitions:
        - { StorageClass: STANDARD_IA, TransitionInDays: 30 }
        - { StorageClass: GLACIER, TransitionInDays: 365 }
      NoncurrentVersionExpiration: { NoncurrentDays: 90 }
    - Id: abort-stale-multipart
      Status: Enabled
      AbortIncompleteMultipartUpload: { DaysAfterInitiation: 7 }
```

## Protecting data

- **Block Public Access**: four settings, on the bucket and on the account, that override
  any public ACL or policy. Keep them on; serve public content through **CloudFront** with
  origin access control instead.
- **Object Ownership** `BucketOwnerEnforced` turns ACLs off: access is decided by policies
  only, which is the default for new buckets.
- **Bucket policies** are resource-based policies: grant other accounts or CloudFront
  access, and deny what must never happen (requests without TLS, uploads without
  encryption, access from outside a VPC endpoint).
- **Encryption**: every new object is encrypted at rest. **SSE-S3** uses keys S3 manages;
  **SSE-KMS** uses a KMS key you control and audit (key policy, CloudTrail, separate
  permission `kms:Decrypt`), with **S3 Bucket Keys** to cut KMS request costs.
- **Versioning** keeps every version of every object: a delete only adds a delete marker,
  so mistakes and ransomware can be undone. Pair it with lifecycle rules for old versions.
- **Object Lock** (WORM): governance or compliance retention; in compliance mode nobody,
  not even root, can delete the object before the date.
- **Replication** (same or cross region) copies new objects to another bucket.

```json
{
  "Sid": "DenyInsecureTransport",
  "Effect": "Deny",
  "Principal": "*",
  "Action": "s3:*",
  "Resource": ["arn:aws:s3:::acme-uploads", "arn:aws:s3:::acme-uploads/*"],
  "Condition": { "Bool": { "aws:SecureTransport": "false" } }
}
```

## Presigned URLs and events

A **presigned URL** carries a signature made with the backend's credentials: whoever holds
it can do that one operation (GET or PUT of one key) until it expires. Browsers and mobile
apps upload and download directly, and your servers never stream the bytes.

```python
url = s3.generate_presigned_url(
    "put_object",
    Params={"Bucket": "acme-uploads", "Key": f"uploads/{user_id}/{uuid4()}.jpg",
            "ContentType": "image/jpeg"},
    ExpiresIn=300,
)
```

**Event notifications** send `s3:ObjectCreated:*` or `s3:ObjectRemoved:*` events to Lambda,
SQS, SNS or EventBridge: generate a thumbnail, index a document, start a pipeline. Never
write the output back to the prefix that triggers the function, or it calls itself forever.

## Summary

- Objects in globally named buckets; keys with prefixes, no real folders; strongly consistent.
- Pick storage classes by access pattern and let lifecycle rules move and expire objects.
- Keep Block Public Access on; use bucket policies, encryption, versioning and Object Lock.
- Presigned URLs give temporary direct access; event notifications react to changes.

## Further reading

- What is Amazon S3?: <https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html>
- Storage classes: <https://docs.aws.amazon.com/AmazonS3/latest/userguide/storage-class-intro.html>
- Managing the lifecycle of objects: <https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html>
- Blocking public access: <https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html>
- Protecting data with encryption: <https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingEncryption.html>
- Presigned URLs: <https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html>
- Event notifications: <https://docs.aws.amazon.com/AmazonS3/latest/userguide/EventNotifications.html>
