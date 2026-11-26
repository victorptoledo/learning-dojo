# Checkpoint 4 (Final Exam): The Well-Architected Review

`service.yaml` is the invoices service as it has run for a year: a queue of invoices, a
Lambda worker that stores each one as a PDF in S3 and a row in DynamoDB. It works. The
Well-Architected review found these problems, one or more per pillar. Fix every finding
in `service.yaml` without breaking the service. The `Environment` parameter (`dev` or
`prod`) and the `IsProd` condition are already there.

**Security**

| Finding | Fix |
| --- | --- |
| the worker's role allows `sqs:*`, `dynamodb:*` and `s3:*` on `*` | keep `AWSLambdaBasicExecutionRole`; the inline policy allows only `sqs:ReceiveMessage`, `DeleteMessage`, `GetQueueAttributes`, `ChangeMessageVisibility` on `InvoiceQueue`; `dynamodb:GetItem`, `PutItem`, `UpdateItem` on `InvoicesTable`; `s3:PutObject` on `invoices/*` in the bucket; `kms:GenerateDataKey` and `kms:Decrypt` on the key |
| invoices are customer data in a default bucket | a new `InvoicesKey` (`AWS::KMS::Key`, rotation on, `Retain`); the bucket blocks all public access and encrypts with `aws:kms` and `!GetAtt InvoicesKey.Arn` |
| plain HTTP reaches the bucket | an `AWS::S3::BucketPolicy` that denies `s3:*` on the bucket and its objects when `aws:SecureTransport` is `false` |
| messages are stored unencrypted | `SqsManagedSseEnabled: true` on both queues |

**Reliability**

| Finding | Fix |
| --- | --- |
| a poison invoice is retried until it expires | `InvoiceDeadLetterQueue` (14 days); redrive after `maxReceiveCount: 5`; the visibility timeout at least six times the function timeout |
| one bad invoice retries the whole batch | the handler already returns `batchItemFailures`: turn on `ReportBatchItemFailures` |
| deleting the stack deletes every invoice | `DeletionPolicy` and `UpdateReplacePolicy` `Retain` on the bucket and the table; versioning on the bucket; point-in-time recovery on the table; `DeletionProtectionEnabled` in prod only |

**Operational excellence**

| Finding | Fix |
| --- | --- |
| nobody hears about failures | `OnCallTopic` (`KmsMasterKeyId: alias/aws/sns`) with an email subscription to `!Ref OnCallEmail` in prod only; `DeadLettersAlarm` (`AWS/SQS` `ApproximateNumberOfMessagesVisible` of the dead-letter queue, `Maximum`, `> 0`) and `QueueBacklogAlarm` (`ApproximateAgeOfOldestMessage` of `InvoiceQueue`, `Maximum`, `> 600` s), both `TreatMissingData: notBreaching`, alarm actions to the topic |
| logs are kept and billed forever | `WorkerLogGroup` named `/aws/lambda/<function>`, 90 days in prod and 14 in dev |

**Performance efficiency**: the mapping reads one message per invocation; read
`BatchSize: 10` with `MaximumBatchingWindowInSeconds: 5`.

**Cost optimization**

| Finding | Fix |
| --- | --- |
| 500 provisioned read and write units sit idle most of the day | `BillingMode: PAY_PER_REQUEST` |
| PDFs older than a month are rarely read | a lifecycle rule to `STANDARD_IA` after 30 days; the S3 Bucket Key on (`BucketKeyEnabled`) |
| the bill cannot be split | tags `service: invoices`, `cost-center: cc-2077` and `environment: !Ref Environment` on the key, bucket, table, both queues, role, function and topic |

**Sustainability**: the worker runs on Graviton (`arm64`); the table expires rows by TTL
on `expires_at`; the bucket expires noncurrent versions after 30 days and aborts
incomplete multipart uploads after 7.

The platform team's rules in `tests/company.guard` (bucket, table, queue, role and log
group rules) must pass too: that is the visible test.

How the check works: cfn-lint validates every resource; the tests resolve the template
as a deployed stack in `dev` and in `prod`, send the worker's requests and the ones it
must not make to the dojo's IAM evaluator, and run the company rules with cfn-guard.

With an AWS account (not needed here):

```powershell
cfn-guard validate --rules tests/company.guard --data service.yaml
aws cloudformation deploy --template-file service.yaml --stack-name invoices-prod --parameter-overrides Environment=prod --capabilities CAPABILITY_IAM
```

There are 41 tests: 1 in `tests/test_visible.py`, 39 hidden ones and the cfn-lint check.
