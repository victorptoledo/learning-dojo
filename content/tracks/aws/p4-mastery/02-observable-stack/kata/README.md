# An Observable, Cost-Aware Stack

Finish `stack.yaml`. The worker (role, queue, dead-letter queue, function, event source
mapping) already runs. Add:

| Resource | Rule |
| --- | --- |
| cost tags | `service: orders` and `cost-center: cc-1042` on `WorkerRole`, `OrdersQueue`, `OrdersDeadLetterQueue`, `WorkerFunction`, `WorkerLogGroup` and `OnCallTopic` |
| `WorkerLogGroup` | `RetentionInDays: 30` |
| an `AWS::Logs::MetricFilter` | on `WorkerLogGroup`, pattern `{ $.level = "ERROR" }`, metric `Acme/Orders` / `ErrorLogs`, value `1` |
| `OnCallTopic` | `KmsMasterKeyId: alias/aws/sns`; one `email` subscription to `!Ref OnCallEmail` |
| an `AWS::SNS::TopicPolicy` | on `OnCallTopic`: `budgets.amazonaws.com` may `sns:Publish` to it only when `aws:SourceAccount` is `!Ref AWS::AccountId`; nobody else |
| `ErrorRateAlarm` | metric math: one expression over the function's `Errors` and `Invocations` (`Sum`, 60 s); `> 1` (percent); 3 of 5 datapoints; alarm and OK actions to the topic |
| `SlowWorkerAlarm` | `AWS/Lambda` `Duration` of the function, `ExtendedStatistic: p99`, `> 5000` ms |
| `QueueBacklogAlarm` | `AWS/SQS` `ApproximateAgeOfOldestMessage` of `OrdersQueue`, `Maximum`, `> 300` s |
| `DeadLettersAlarm` | `AWS/SQS` `ApproximateNumberOfMessagesVisible` of `OrdersDeadLetterQueue`, `Maximum`, `> 0` |
| `ErrorLogsAlarm` | `Acme/Orders` `ErrorLogs`, `Sum` |
| every alarm | `TreatMissingData: notBreaching`; `AlarmActions` is the topic |
| an `AWS::Budgets::Budget` | `COST`, `MONTHLY`, limit `!Ref MonthlyBudgetUsd` USD, `CostFilters: { TagKeyValue: ["user:service$orders"] }`; notify the topic at 80 % `ACTUAL` and 100 % `FORECASTED` |

SQS alarms use the dimension `QueueName` with `!GetAtt <Queue>.QueueName`; Lambda alarms
use `FunctionName` with `!Ref WorkerFunction`.

How the check works: cfn-lint validates every resource (a wrong alarm property or budget
field fails `cfn-lint::stack_yaml`), the tests read the template, and the topic policy is
evaluated by the dojo's IAM evaluator against the budgets service, other services and
other accounts.

There are 22 tests: 1 in `tests/test_visible.py`, 20 hidden ones and the cfn-lint check.
