# Checkpoint 2: A Serverless Orders Service

Finish `template.yaml`, the SAM template of the orders service. `src/api.py` (an HTTP API)
and `src/worker.py` (a queue worker) are given and work; they read `TABLE_NAME` and
`QUEUE_URL` from the environment.

| Piece | Rule |
| --- | --- |
| `Globals.Function` | `Runtime: python3.14`, `CodeUri: src/`, `Architectures: [arm64]`, `MemorySize: 256`, `Timeout: 10`, `Tracing: Active`, `TABLE_NAME: !Ref OrdersTable` |
| `OrdersTable` | on-demand, keys `pk`/`sk` (given); `DeletionPolicy` and `UpdateReplacePolicy` `Retain`; point-in-time recovery on; server-side encryption on |
| `OrderEventsDeadLetterQueue` | `MessageRetentionPeriod: 1209600` (14 days), `SqsManagedSseEnabled: true` |
| `OrderEventsQueue` | `VisibilityTimeout: 60` (six times the function timeout), `SqsManagedSseEnabled: true`, redrive to the dead-letter queue after `maxReceiveCount: 5` |
| `ApiFunction` | `Handler: api.handler`; `QUEUE_URL: !Ref OrderEventsQueue`; HttpApi events `POST /orders` and `GET /orders/{id}`; policies `DynamoDBCrudPolicy` on the table and `SQSSendMessagePolicy` on the queue, nothing else |
| `WorkerFunction` | `Handler: worker.handler`; an SQS event on `!GetAtt OrderEventsQueue.Arn` with `BatchSize: 10` and `FunctionResponseTypes: [ReportBatchItemFailures]`; policy `DynamoDBCrudPolicy` on the table |
| log groups | one `AWS::Logs::LogGroup` per function, named `!Sub "/aws/lambda/${ApiFunction}"` (and `WorkerFunction`), `RetentionInDays: 30` |
| outputs | `ApiUrl` (given) and `DeadLetterQueueUrl: !Ref OrderEventsDeadLetterQueue` |

No managed policies such as `AmazonDynamoDBFullAccess`: SAM policy templates expand into
statements limited to the one table or queue.

How the check works: cfn-lint expands the SAM transform and validates every generated
resource; pytest then checks the template's rules; ruff checks the two given modules.

With an AWS account (not needed here):

```powershell
sam validate --lint
sam build; sam deploy --guided
curl -X POST "$ApiUrl/orders" -d '{"items": ["sku-1"]}'
```

There are 21 tests: 1 in `tests/test_visible.py`, 17 hidden ones, the cfn-lint check and two
ruff checks.
