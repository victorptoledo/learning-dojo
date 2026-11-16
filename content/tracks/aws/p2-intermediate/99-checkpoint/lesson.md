# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 8 right.
2. **The integration kata** (press **Enter**): finish the SAM template of a serverless
   orders service: an HTTP API function, a queue worker, a protected DynamoDB table, a
   queue with a dead-letter queue, least-privilege policies and log retention. The Python
   code in `src/` is given; the template is yours.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Databases | RDS, Aurora or DynamoDB for this workload? What does Multi-AZ protect, and what does a read replica? How do keys follow access patterns? |
| Lambda and serverless | What is a cold start? Which event sources poll and which push? Why a DLQ, and why a visibility timeout of six times the function timeout? |
| Containers | Task role or execution role? Fargate or EC2 capacity? ECS or EKS? |
| CloudWatch | Metric, log, alarm, trace: which answers which question? What does log retention cost if you forget it? |

> [!TIP]
> cfn-lint expands the SAM transform offline, so a wrong event property or policy template
> name fails the `cfn-lint::template_yaml` test before the other tests even matter.
