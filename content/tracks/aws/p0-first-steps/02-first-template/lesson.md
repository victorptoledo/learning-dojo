# Your First Template: a Private, Encrypted S3 Bucket

Your first piece of infrastructure as code: one S3 bucket, declared in a CloudFormation
template. Press **Enter** to create the kata folder and open it, edit `template.yaml`, and
save. The dojo lints the template with **cfn-lint** (which knows the schema of every AWS
resource type, so a misspelt property fails) and runs tests over the parsed template. No
AWS account is involved.

A CloudFormation template is YAML (or JSON) with a few top-level sections:

```yaml
AWSTemplateFormatVersion: "2010-09-09"
Description: What this stack is for
Parameters: {}     # inputs given when the stack is created (Phase 1)
Resources:         # the only required section: logical id -> resource
  MyBucket:
    Type: AWS::S3::Bucket
    Properties: {}
Outputs: {}        # values the stack exposes: names, ARNs, URLs
```

`!Ref MyBucket` and `!GetAtt MyBucket.Arn` are **intrinsic functions**: CloudFormation
fills them in when it creates the stack (for a bucket, `Ref` gives its name). If you have
an account, the same file deploys with
`aws cloudformation deploy --template-file template.yaml --stack-name dojo-reports`.
