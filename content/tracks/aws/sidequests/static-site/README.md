# Sidequest: Private static site

A small studio wants its portfolio online: plain HTML, CSS and images, no servers. The files
go to an S3 bucket and visitors reach them through CloudFront, over HTTPS, from the edge.
Nobody may read the bucket directly, not even with a leaked object URL. You build it in
three stages:

1. `site.yaml`, a CloudFormation template with a private, versioned and encrypted bucket that
   survives the stack;
2. a CloudFront distribution with origin access control, and a bucket policy that lets only
   that distribution read, and only over TLS;
3. `publish.py`, a script that uploads a folder with the right content types and cache
   headers, skips what did not change, deletes what disappeared and says which paths
   CloudFront must invalidate.

How it is checked, with no AWS account:

- The template is linted by **cfn-lint** against the AWS resource schemas, and the tests read
  it as a deployed stack would: parameters, conditions and intrinsic functions (`!Ref`,
  `!GetAtt`, `!Sub`) are resolved to made-up but realistic names and ARNs (account
  `111122223333`, region `eu-west-1`, stack `dojo`, so `SiteBucket` is named
  `dojo-sitebucket`).
- The bucket policy is resolved the same way and run through the **dojo's IAM policy
  evaluator** with a matrix of requests: CloudFront with the right and the wrong
  distribution, other accounts, uploads, requests without TLS.
- `publish.py` runs against **moto**, an in-memory S3, and the requests it sends are
  recorded.

With an AWS account (not needed here; CloudFront and S3 cost very little at this size):

```powershell
aws cloudformation deploy --template-file site.yaml --stack-name portfolio
python -c "import boto3, publish; print(publish.publish(boto3.client('s3'), '<BucketName>', 'site'))"
```

When you are done, `[A]` copies a review prompt: ask an AI about your template and script.
