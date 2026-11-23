Start with the bucket. Everything goes in `site.yaml`; the starter declares `SiteBucket` with
no properties, which CloudFormation would happily create with versioning off, no lifecycle
and a bucket that disappears with the stack.

| Piece | Rule |
| --- | --- |
| name | no `BucketName`: let CloudFormation pick a unique one (a fixed name blocks replacements and may already be taken in another account) |
| public access | `PublicAccessBlockConfiguration` with all four settings `true` |
| ACLs | `OwnershipControls` with the single rule `ObjectOwnership: BucketOwnerEnforced` (ACLs disabled, the bucket owns every object) |
| encryption | `BucketEncryption` with one rule, `SSEAlgorithm: AES256` (SSE-S3; CloudFront's origin access control reads it without a KMS key policy) |
| versioning | `VersioningConfiguration: { Status: Enabled }`, so an overwritten or deleted page can be restored |
| lifecycle | enabled rules that expire noncurrent versions after 30 days (`NoncurrentVersionExpiration: { NoncurrentDays: 30 }`) and abort incomplete multipart uploads after 7 days; never expire current versions |
| deletion | `DeletionPolicy: Retain` and `UpdateReplacePolicy: Retain`: deleting the stack or replacing the bucket never deletes the site |
| outputs | `BucketName` (`!Ref SiteBucket`) and `BucketArn` (`!GetAtt SiteBucket.Arn`) |

The check runs cfn-lint on `site.yaml` too: a misspelt property or a wrong value type fails
it, with the line number.

> [!TIP]
> `DeletionPolicy` and `UpdateReplacePolicy` are attributes of the resource, next to `Type`
> and `Properties`, not properties of the bucket.
