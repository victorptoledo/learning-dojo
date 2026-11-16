# Cost: Pricing Models, Savings Plans, Spot, Tags and Budgets

In the cloud, architecture *is* cost: every design choice shows up on the bill a month
later. Engineers who can read that bill and change it are rare and valuable.

## How AWS charges

| Dimension | Examples |
| --- | --- |
| compute time | EC2 per second, Fargate per vCPU and GB per second, Lambda per GB-second |
| provisioned capacity | EBS GB-month and IOPS, RDS instance hours, provisioned DynamoDB capacity |
| storage | S3 GB-month by storage class, snapshots, backups, CloudWatch Logs |
| requests | S3 PUT/GET, Lambda invocations, API Gateway calls, DynamoDB on-demand reads and writes, KMS requests |
| **data transfer** | out to the internet, between regions, **between AZs**, through NAT gateways (per GB processed) |
| hourly fixed costs | NAT gateways, load balancers, interface endpoints, public IPv4 addresses |

Data transfer is the surprise in most bills: traffic **into** AWS is free, **out** to the
internet costs, traffic between AZs costs in both directions, and every GB through a NAT
gateway is charged on top. CloudFront in front of origins often lowers transfer cost.

## Purchase options

| Option | Commitment | Savings | Fits |
| --- | --- | --- | --- |
| On-Demand | none | — | spiky, new or short-lived work |
| **Compute Savings Plans** | a spend per hour for 1 or 3 years | up to ~66 % | EC2 of any family and region, Fargate and Lambda |
| EC2 Instance Savings Plans | one family in one region | up to ~72 % | stable fleets |
| Reserved Instances | an instance type (and RDS, ElastiCache, OpenSearch, Redshift reservations) | up to ~72 % | databases and other reserved services |
| **Spot** | none, can be interrupted with 2 minutes' notice | up to ~90 % | batch, CI, stateless workers, mixed ASGs |

Commit only to the **baseline** you will run anyway, and only **after** right-sizing;
cover the rest with On-Demand and Spot. Graviton instances add 20–40 % better
price-performance on top.

## Visibility

- **Cost allocation tags**: tag resources (`service`, `team`, `environment`,
  `cost-center`) and **activate** the tags in the billing console; **tag policies** in
  Organizations enforce names and values. Untagged spend is the first thing to drive down.
- **Cost Explorer**: spend by service, account, tag and usage type, with forecasts and
  Savings Plans recommendations.
- **Cost and Usage Report** (CUR 2.0 through Data Exports): every line item, to S3, queried
  with Athena or QuickSight for detailed analysis and showback.
- **AWS Budgets**: alerts (and actions) on actual or forecast cost and usage, per account,
  service or tag. **Cost Anomaly Detection** alerts on unusual spend without thresholds.
- **Compute Optimizer** and **Trusted Advisor** recommend right-sizing and flag idle
  resources.

```yaml
OrdersBudget:
  Type: AWS::Budgets::Budget
  Properties:
    Budget:
      BudgetType: COST
      TimeUnit: MONTHLY
      BudgetLimit: { Amount: 500, Unit: USD }
      CostFilters: { TagKeyValue: ["user:service$orders"] }
    NotificationsWithSubscribers:
      - Notification: { NotificationType: FORECASTED, ComparisonOperator: GREATER_THAN, Threshold: 100 }
        Subscribers: [{ SubscriptionType: SNS, Address: !Ref CostAlertsTopic }]
```

## Where savings usually hide

1. **Idle and forgotten resources**: unattached EBS volumes, old snapshots and AMIs, idle
   load balancers, unused Elastic IPs and public IPv4 addresses, stopped-but-paid RDS.
2. **Over-sized compute**: instances at 5 % CPU, Lambda memory never tuned (use Lambda
   Power Tuning), Fargate tasks with twice the memory they use.
3. **Non-production running 24/7**: stop dev and test at night and on weekends (that is
   ~65 % of the hours).
4. **Storage without a lifecycle**: logs with no retention, S3 data in Standard forever,
   gp2 volumes that would be cheaper as gp3.
5. **Network paths**: S3 and DynamoDB traffic through NAT gateways (use gateway endpoints),
   chatty cross-AZ traffic, missing CloudFront caching.
6. **Commitments**: steady baseline still on On-Demand.

## FinOps

Cost is a shared, continuous practice: each team sees its own spend (showback), cost is a
non-functional requirement in design reviews ("what does this cost per 1,000 orders?"),
and the cost optimization pillar of the Well-Architected Framework (next phase) gives the
checklist. Measure **unit cost** (cost per order, per active user) rather than the total
alone: a growing bill can be good news.

## Summary

- Bills add compute, capacity, storage, requests and data transfer; NAT and cross-AZ traffic surprise people.
- Savings Plans and RIs for the baseline after right-sizing; Spot for interruptible work.
- Tag everything, activate the tags, and use Cost Explorer, CUR, Budgets and anomaly detection.
- Delete idle resources, right-size, schedule non-production, add lifecycles and endpoints.
- Track unit cost and make teams own their spend.

## Further reading

- AWS pricing overview (whitepaper): <https://docs.aws.amazon.com/whitepapers/latest/how-aws-pricing-works/how-aws-pricing-works.html>
- Cost optimization pillar: <https://docs.aws.amazon.com/wellarchitected/latest/cost-optimization-pillar/welcome.html>
- Savings Plans user guide: <https://docs.aws.amazon.com/savingsplans/latest/userguide/what-is-savings-plans.html>
- Using cost allocation tags: <https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/cost-alloc-tags.html>
- AWS Cost Explorer: <https://docs.aws.amazon.com/cost-management/latest/userguide/ce-what-is.html>
- Amazon EC2 Spot Instances: <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-spot-instances.html>
- Overview of data transfer costs for common architectures: <https://aws.amazon.com/blogs/architecture/overview-of-data-transfer-costs-for-common-architectures/>
