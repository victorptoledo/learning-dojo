# Lambda and Serverless: API Gateway, SQS, SNS, EventBridge and Step Functions

**AWS Lambda** runs your function when an event arrives and bills per millisecond of
execution. No servers, no idle cost, scaling by itself. "Serverless" goes further: build
from managed services (API Gateway, SQS, DynamoDB, Step Functions) so you write mostly
business logic.

## The execution model

```python
import json
import os

import boto3

table = boto3.resource("dynamodb").Table(os.environ["TABLE_NAME"])   # init: once per environment


def handler(event, context):                                         # per invocation
    order = json.loads(event["body"])
    table.put_item(Item=order)
    return {"statusCode": 201, "body": json.dumps({"id": order["id"]})}
```

- An **execution environment** (a Firecracker micro-VM) loads your code, runs the **init**
  code outside the handler once, then serves invocations **one at a time**, and is reused
  while warm. Create SDK clients and connection pools at init.
- A **cold start** is the creation of a new environment: from tens of milliseconds to
  seconds (big Java or .NET apps). Shrink it with small packages, **SnapStart** (Java,
  Python, .NET: restore a snapshot of the initialised environment) or **provisioned
  concurrency** (environments kept warm, billed).
- **Concurrency** = requests per second × duration. The account has a regional limit
  (1,000 by default); **reserved concurrency** guarantees and caps one function's share,
  which also protects a fragile downstream database.
- **Memory** (128 MB to 10 GB) also sets CPU; more memory often runs faster *and* cheaper.
  The **timeout** is at most 15 minutes: longer work belongs in Step Functions, ECS or Batch.
- The function's **execution role** gives it its permissions; `/tmp` gives up to 10 GB of
  scratch disk; layers and container images (up to 10 GB) package dependencies.

## How functions are invoked

| Type | Sources | Retries and failures |
| --- | --- | --- |
| **Synchronous** | API Gateway, function URLs, ALB, the SDK `Invoke` | the caller gets the error and decides |
| **Asynchronous** | S3 events, SNS, EventBridge | Lambda retries twice; then **on-failure destination** or DLQ |
| **Poll-based** (event source mappings) | SQS, Kinesis, DynamoDB Streams, Kafka | Lambda polls and calls you with batches; SQS retries per message until `maxReceiveCount`, then the queue's DLQ; **partial batch responses** retry only failed items |

Every source can deliver an event more than once: handlers must be **idempotent**.

## API Gateway

**HTTP APIs** (cheaper, simpler: JWT authorizers, CORS, Lambda and HTTP integrations) and
**REST APIs** (usage plans and API keys, request validation, WAF, private APIs, caching)
put HTTPS endpoints in front of functions. **Function URLs** give a single function a URL
with IAM or no auth. Throttling, custom domains and stages come built in.

## Decoupling: SQS, SNS and EventBridge

| Service | Model | Use it for |
| --- | --- | --- |
| **SQS** | a queue: one consumer group pulls, messages kept up to 14 days | buffering, smoothing load, work queues; FIFO queues for order and exactly-once processing within a group |
| **SNS** | a topic: push to many subscribers (SQS, Lambda, HTTPS, email) | simple fan-out |
| **EventBridge** | an event bus with content-based **rules**, schema registry, archive and replay, cross-account buses, and **Scheduler** | events between services and teams, SaaS and AWS service events, cron jobs |

A common pattern is **EventBridge (or SNS) → one SQS queue per consumer → Lambda**: each
consumer gets its own buffer, retry policy and DLQ, and a slow consumer never slows the
others.

```json
{ "source": ["acme.orders"], "detail-type": ["OrderPlaced"], "detail": { "total": [{ "numeric": [">", 1000] }] } }
```

## Step Functions

**AWS Step Functions** runs **state machines** written in Amazon States Language (JSON, or
the visual Workflow Studio): `Task` (call Lambda or 200+ AWS APIs directly), `Choice`,
`Parallel`, `Map` (fan out over items), `Wait`, with `Retry` and `Catch` per state.
Standard workflows run up to a year with exactly-once steps and full history; Express
workflows are cheaper and faster for high-volume, short flows. Use it for sagas: when
"charge" fails after "reserve", a `Catch` runs the compensating "release".

## Packaging with SAM

```yaml
Transform: AWS::Serverless-2016-10-31
Globals:
  Function: { Runtime: python3.14, Architectures: [arm64], Timeout: 10, Tracing: Active }
Resources:
  Worker:
    Type: AWS::Serverless::Function
    Properties:
      Handler: worker.handler
      CodeUri: src/
      Policies: [DynamoDBCrudPolicy: { TableName: !Ref Table }]
      Events:
        Jobs:
          Type: SQS
          Properties: { Queue: !GetAtt Jobs.Arn, BatchSize: 10, FunctionResponseTypes: [ReportBatchItemFailures] }
```

`sam build`, `sam local invoke` (runs the function in a container), `sam deploy` and
`sam logs` cover the inner loop. **Powertools for AWS Lambda** (Python, Java, TypeScript,
.NET) adds structured logging, metrics, tracing, idempotency and batch helpers.

## Summary

- Lambda reuses execution environments; init once, keep handlers small and idempotent.
- Cold starts, concurrency, memory and the 15-minute timeout shape the design.
- Synchronous, asynchronous and poll-based invocations retry differently; plan where failures go.
- SQS buffers, SNS fans out, EventBridge routes events by content; Step Functions runs workflows.
- SAM packages functions, events and least-privilege policy templates.

## Further reading

- Lambda execution environment lifecycle: <https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtime-environment.html>
- Understanding Lambda function scaling: <https://docs.aws.amazon.com/lambda/latest/dg/lambda-concurrency.html>
- Using Lambda with Amazon SQS: <https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html>
- Amazon EventBridge user guide: <https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-what-is.html>
- AWS Step Functions developer guide: <https://docs.aws.amazon.com/step-functions/latest/dg/welcome.html>
- AWS SAM developer guide: <https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html>
- Powertools for AWS Lambda (Python): <https://docs.powertools.aws.dev/lambda/python/latest/>
