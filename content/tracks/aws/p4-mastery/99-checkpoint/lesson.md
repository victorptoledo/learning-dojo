# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards the
   Well-Architected Framework and running AWS in production. You need 7 right.
2. **The integration kata** (press **Enter**): the invoices service has run for a year
   and works. Its Well-Architected review found problems in all six pillars. Fix every
   finding in one CloudFormation template that deploys to dev and prod: least privilege
   checked request by request, encryption with your own key, a dead-letter queue and
   partial batch failures, data that outlives the stack, alarms that page only in prod,
   on-demand capacity, lifecycle rules, cost tags and Graviton. The company's cfn-guard
   rules must pass too.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| IAM and least privilege | Which exact actions, on which ARNs, does this code call? |
| S3 and security services | Who can read the bucket? Over which transport? With which key? |
| Lambda and serverless | What happens to a message that fails five times? What does a partial batch response change? |
| CloudWatch | Which alarm tells you a user is hurting? Who gets paged, and from which environment? |
| High availability and cost | What survives `aws cloudformation delete-stack`? What do idle provisioned units cost? |
| The Well-Architected Framework | Which pillar does each finding belong to, and which trade-off does its fix make? |
| Running AWS in production | How does one template behave in dev and in prod? |

> [!TIP]
> Fix one pillar at a time and run the checks after each. The worker's permissions are
> tested in both directions: every request it makes must pass, and the ones it never
> makes must not. Tightening too far breaks the service as surely as leaving `*`.
