# An Observable, Cost-Aware Stack

The orders worker works. Nobody would notice if it stopped, nobody knows what it costs,
and its logs are kept forever. The Well-Architected pillars of **operational excellence**,
**reliability** and **cost optimization** turn into concrete resources in its template:

- **Alarms on symptoms**: an error *rate* (metric math over `Errors` and `Invocations`),
  p99 duration, the age of the oldest message (the queue is not draining) and any message
  in the dead-letter queue. Each one goes to an on-call SNS topic.
- **Logs that pay for themselves**: 30 days of retention, and a metric filter that turns
  JSON lines with `"level": "ERROR"` into a custom metric.
- **Cost you can see**: `service` and `cost-center` tags on every resource (activated as
  cost allocation tags in the billing console), and an AWS Budget filtered on the tag,
  alerting on 80 % of actual spend and 100 % of forecast spend.

The budget publishes to the same topic, so the topic needs a **resource policy** that lets
`budgets.amazonaws.com` publish for this account only. The tests evaluate that policy with
the dojo's IAM evaluator, like Phase 3's guardrails.
