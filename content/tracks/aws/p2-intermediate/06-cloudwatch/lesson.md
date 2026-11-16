# Observability: CloudWatch Metrics, Logs, Alarms and X-Ray

You cannot SSH into Lambda or a Fargate task to "have a look". You learn what a system is
doing from what it emits: **metrics** (numbers over time), **logs** (events with detail)
and **traces** (one request across services). **Amazon CloudWatch** collects the first two
for every AWS service; **X-Ray** and OpenTelemetry handle traces.

## Metrics

A metric lives in a **namespace** (`AWS/Lambda`, `AWS/SQS`, `Acme/Orders`), is identified by
**dimensions** (`FunctionName=orders-api`) and is read with a **statistic** over a
**period**: `Sum`, `Average`, `Minimum`, `Maximum`, `SampleCount` and **percentiles**
(`p50`, `p99`). Averages hide pain: alarm on `p99` latency, not on the average.

Metrics worth knowing:

| Service | Metrics |
| --- | --- |
| Lambda | `Invocations`, `Errors`, `Throttles`, `Duration`, `ConcurrentExecutions`, `IteratorAge` |
| API Gateway / ALB | `Count`/`RequestCount`, `4XXError`, `5XXError`/`HTTPCode_Target_5XX_Count`, `Latency`/`TargetResponseTime` |
| SQS | `ApproximateNumberOfMessagesVisible`, `ApproximateAgeOfOldestMessage` |
| DynamoDB | `ThrottledRequests`, `ConsumedReadCapacityUnits`, `SuccessfulRequestLatency` |
| RDS | `CPUUtilization`, `FreeableMemory`, `DatabaseConnections`, `ReplicaLag` |

**Custom metrics** come from `PutMetricData`, or cheaply from logs with the **embedded
metric format** (EMF): a JSON log line that CloudWatch turns into metrics, which Powertools
writes for you. Each unique combination of dimensions is a separate (billed) metric: never
put a user id in a dimension. EC2 memory and disk need the **CloudWatch agent**.

## Logs

Logs go to **log groups** (`/aws/lambda/orders-api`) made of **log streams**. Rules:

- Log **structured JSON** with a `level`, a `requestId` or correlation id and the fields you
  will search on; never secrets or personal data.
- Set **retention** on every log group: the default is *never expire*, and storage is billed.
  Lambda creates its group on first run without retention, so declare it in the template.
- **Logs Insights** queries across groups:

```text
fields @timestamp, level, message, orderId
| filter level = "ERROR"
| stats count(*) as errors by bin(5m), errorType
| sort errors desc
```

- **Metric filters** turn matching log lines into metrics (count `"level":"ERROR"`); you
  can alarm on them.
- **Subscription filters** stream logs to Lambda, Firehose or OpenSearch; the Infrequent
  Access log class and S3 exports cut cost for logs you rarely read.

## Alarms

An alarm watches one metric (or a **metric math** expression such as error rate =
`errors / invocations`) and goes to `ALARM` when **M out of N** datapoints breach a
threshold. It notifies an **SNS** topic (email, PagerDuty, Amazon Q Developer in chat
apps), or acts: scale, stop an instance, roll back a deployment.

```yaml
ErrorRateAlarm:
  Type: AWS::CloudWatch::Alarm
  Properties:
    AlarmDescription: Orders API returns 5xx to more than 1 % of requests
    ComparisonOperator: GreaterThanThreshold
    Threshold: 1
    EvaluationPeriods: 5
    DatapointsToAlarm: 3
    TreatMissingData: notBreaching
    AlarmActions: [!Ref OnCallTopic]
    Metrics:
      - Id: rate
        Expression: "100 * errors / requests"
      - Id: errors
        ReturnData: false
        MetricStat: { Metric: { Namespace: AWS/ApiGateway, MetricName: 5xx, Dimensions: [{ Name: ApiId, Value: !Ref Api }] }, Period: 60, Stat: Sum }
      - Id: requests
        ReturnData: false
        MetricStat: { Metric: { Namespace: AWS/ApiGateway, MetricName: Count, Dimensions: [{ Name: ApiId, Value: !Ref Api }] }, Period: 60, Stat: Sum }
```

Alarm on **symptoms users feel** (errors, latency, a queue that stops draining), with
resource metrics on dashboards to explain them. `TreatMissingData` matters: a function that
stops being invoked produces no `Errors` datapoints at all. **Composite alarms** combine
alarms to cut noise; **anomaly detection** learns a metric's normal band. **Synthetics
canaries** probe endpoints from outside; **Application Signals** gives SLOs per service.

## Traces

**AWS X-Ray** (now fed through OpenTelemetry, with the **AWS Distro for OpenTelemetry**)
follows a request through API Gateway, Lambda, the SDK calls to DynamoDB and SQS, and
downstream HTTP, and shows a service map and where the time went. Turn on active tracing
in Lambda (`Tracing: Active`) and instrument the SDK; sample, do not trace everything.

## CloudWatch is not CloudTrail

**CloudTrail** records **API calls** to AWS: who deleted the bucket, who changed the
security group, from which IP, with which role. CloudWatch records what your
**workloads** do. You need both; Phase 3 covers CloudTrail.

## Summary

- Metrics: namespace, dimensions, statistic, period; alarm on percentiles and rates.
- Logs: structured JSON, retention on every group, Logs Insights, metric filters.
- Alarms: symptoms, M of N datapoints, deliberate missing-data handling, SNS to on-call.
- Traces show where a request spends its time; CloudTrail shows who called which API.

## Further reading

- Amazon CloudWatch concepts: <https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html>
- Using Amazon CloudWatch alarms: <https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/AlarmThatSendsEmail.html>
- CloudWatch Logs Insights query syntax: <https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CWL_QuerySyntax.html>
- Embedded metric format: <https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format.html>
- Creating metric filters: <https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/MonitoringLogData.html>
- AWS X-Ray developer guide: <https://docs.aws.amazon.com/xray/latest/devguide/aws-xray.html>
