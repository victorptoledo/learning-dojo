# Your First Template

Finish `template.yaml`: a stack with one S3 bucket for reports that is private, keeps old
versions of every object and encrypts everything it stores.

| Rule | Value |
| --- | --- |
| one bucket | logical id `ReportsBucket`, type `AWS::S3::Bucket` |
| keep the data | `DeletionPolicy: Retain` and `UpdateReplacePolicy: Retain` on the bucket |
| versioning | `VersioningConfiguration.Status` is `Enabled` |
| default encryption | one rule with `SSEAlgorithm: AES256` (SSE-S3) |
| no public access | the four `PublicAccessBlockConfiguration` settings are `true` |
| no fixed name | leave `BucketName` out: CloudFormation generates a unique one |
| tag | `project` = `dojo` |
| outputs | `BucketName` = `!Ref ReportsBucket`, `BucketArn` = `!GetAtt ReportsBucket.Arn` |

How the check works: the template must parse (CloudFormation short forms such as `!Ref`
are fine), **cfn-lint** must report no errors or warnings (the `cfn-lint::template_yaml`
test), then pytest reads the template and checks each rule.

With an AWS account (not needed here):

```powershell
aws cloudformation validate-template --template-body file://template.yaml
aws cloudformation deploy --template-file template.yaml --stack-name dojo-reports
aws cloudformation describe-stacks --stack-name dojo-reports --query "Stacks[0].Outputs"
```

There are 12 tests: 1 in `tests/test_visible.py`, 10 hidden ones and the cfn-lint check.
