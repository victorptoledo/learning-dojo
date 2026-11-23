The instances need an identity. Add the parameter `ArtifactBucket` (the bucket where the
release pipeline writes builds, under `releases/`) and:

| Resource | Rule |
| --- | --- |
| `AppLogGroup` (`AWS::Logs::LogGroup`) | the application's logs, kept 90 days |
| `InstanceRole` (`AWS::IAM::Role`) | trusted by `ec2.amazonaws.com` only; the one managed policy `arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore` (Session Manager, no SSH); and one inline policy, below |
| `InstanceProfile` | holds `InstanceRole`; the launch template uses it (`IamInstanceProfile: { Arn: !GetAtt InstanceProfile.Arn }`) |

The inline policy allows exactly this, with each resource named:

1. `secretsmanager:GetSecretValue` on the database's managed secret,
   `!GetAtt Database.MasterUserSecret.SecretArn`;
2. `kms:Decrypt` on `DataKey`, **only** when the call comes through Secrets Manager
   (`kms:ViaService` = `secretsmanager.${AWS::Region}.amazonaws.com`): the application can
   read the secret, but cannot decrypt snapshots or anything else with the key directly;
3. `s3:GetObject` on `arn:aws:s3:::<ArtifactBucket>/releases/*`;
4. `logs:CreateLogStream` and `logs:PutLogEvents` on `AppLogGroup`.

The tests resolve the role and ask the IAM evaluator dozens of questions: everything above
is allowed, and the near misses are not: another secret, `kms:Decrypt` called directly or
through another service, `kms:Encrypt`, a build outside `releases/`, uploading a build,
another log group, `iam:PassRole`, `ec2:RunInstances`, `sts:AssumeRole`... No statement may
use a `*` action or a `*` resource.

> [!TIP]
> When the application calls `GetSecretValue`, Secrets Manager calls KMS on its behalf, and
> KMS sees the caller's identity with the condition key `kms:ViaService` set to
> `secretsmanager.<region>.amazonaws.com`. The application never calls KMS itself.
