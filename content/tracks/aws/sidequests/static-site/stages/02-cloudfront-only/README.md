The bucket is locked; now give it exactly one reader. Add to `site.yaml`:

| Resource | Rule |
| --- | --- |
| `SiteOriginAccessControl` (`AWS::CloudFront::OriginAccessControl`) | origin type `s3`, `SigningBehavior: always`, `SigningProtocol: sigv4`, and a `Name` |
| `SiteDistribution` (`AWS::CloudFront::Distribution`) | enabled; `DefaultRootObject: index.html`; one origin whose `DomainName` is `!GetAtt SiteBucket.RegionalDomainName`, with `OriginAccessControlId: !GetAtt SiteOriginAccessControl.Id` and `S3OriginConfig: { OriginAccessIdentity: "" }` (the legacy OAI is not used) |
| default cache behaviour | targets that origin's `Id`; `ViewerProtocolPolicy: redirect-to-https`; only `GET` and `HEAD` (and `OPTIONS`); `Compress: true`; the managed *CachingOptimized* policy (`CachePolicyId: 658327ea-f89d-4fab-a63d-7e88639e58f6`) instead of the legacy `ForwardedValues` |
| error pages | `CustomErrorResponses` for **403 and 404**, both answered with `/404.html` and `ResponseCode: 404` |
| the rest | `HttpVersion: http2and3`, `PriceClass: PriceClass_100` |
| `SiteBucketPolicy` (`AWS::S3::BucketPolicy`) | on `!Ref SiteBucket`: allow the service principal `cloudfront.amazonaws.com` to `s3:GetObject` on the objects, **only** when `AWS:SourceArn` is this distribution's ARN (`arn:aws:cloudfront::${AWS::AccountId}:distribution/${SiteDistribution}`); deny every `s3:*` request on the bucket and its objects when `aws:SecureTransport` is `false` |
| outputs | add `DistributionId` (`!Ref SiteDistribution`) and `SiteUrl` (`!Sub "https://${SiteDistribution.DomainName}"`) |

The tests resolve your bucket policy and ask the IAM evaluator, among others:

- CloudFront reading `index.html` for this distribution, over TLS: allowed.
- CloudFront for **another** distribution of the same account, or with no source ARN: denied
  (this is the confused deputy the condition prevents).
- CloudFront listing the bucket, uploading or deleting: denied.
- Someone in another account: denied.
- An administrator of this account with `s3:*`, over plain HTTP: denied by your explicit
  deny; the same request over TLS: allowed (the deny must not lock the owners out).

> [!TIP]
> Why both 403 and 404? Without `s3:ListBucket`, S3 answers a missing key with **403**
> Access Denied instead of 404, so it does not reveal which keys exist. CloudFront has to
> turn both into your 404 page.
