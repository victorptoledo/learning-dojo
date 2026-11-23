Finance signs off last. They want to see, per cost center, what the shop spends, hear about
it before the money is gone, and know the bill before the first deployment.

**Tags and budget** (in `app.yaml`). Add the parameters `CostCenter` (`AllowedPattern`
`^CC-\d{4}$`), `MonthlyBudget` (`Type: Number`, default `250`, in dollars) and `AlertEmail`.

| Resource | Rule |
| --- | --- |
| tags | every security group, the load balancer, the target group, the database, the key, the log group, the role and the topic carry the tag `cost-center` = `!Ref CostCenter`; the Auto Scaling group too, with `PropagateAtLaunch: true` so every instance (and its bill) gets it. Turn the tag on as a cost allocation tag in Billing, and the bill splits by it |
| `AlertTopic` (`AWS::SNS::Topic`) | an `email` subscription to `AlertEmail` |
| `AlertTopicPolicy` | lets the service principal `budgets.amazonaws.com` `sns:Publish` to the topic, only for this account (`aws:SourceAccount`) |
| `MonthlyCost` (`AWS::Budgets::Budget`) | a monthly `COST` budget of `MonthlyBudget` USD, filtered to the cost center (`CostFilters: { TagKeyValue: ["user:cost-center$CC-1234"] }`, built with `!Sub`); two notifications to the topic: **actual** spend above 80% and **forecasted** spend above 100% |

**`cost.py`** estimates the stack before it exists. Write two functions:

- `monthly_cost(template, parameters, prices)` returns
  `{"lines": {"AutoScalingGroup": 42.07, "Database": 119.22, "LoadBalancer": 18.4, "DataKey": 1.0}, "total": 180.69}`:
  one line per resource that costs money, in dollars a month, rounded to cents (the total
  is the rounded sum of the exact lines). `template` is `app.yaml` already parsed;
  `parameters` overrides its defaults; `prices` looks like `prices.json` (this stage added
  it; real prices change, the tests use other ones too).
- `fits_budget(template, parameters, prices)` says whether the total is at most the
  template's `MonthlyBudget`.

| Line | Price |
| --- | --- |
| each Auto Scaling group | `MinSize` × (the instance type's hourly price × `hours_per_month` + the GiB of its launch template's EBS volumes × `ebs_gp3_gb_month`) |
| each RDS instance | (the class's hourly price × hours + `AllocatedStorage` × `rds_gp3_gb_month`), **twice** when `MultiAZ` (a standby is a second database); plus `secret_month` when RDS manages the password |
| each load balancer | `alb_hour` × hours (traffic is not counted) |
| each KMS key | `kms_key_month` |

Read the instance type, class, sizes and counts from the template and its parameters,
never typed into `cost.py`: the tests change the parameters, the prices and the template
itself. Inside the dojo's image, `from dojo_aws.stack import Stack` resolves `!Ref` and
parameter defaults for you (`Stack(template, parameters).value(...)`); resolving the few
references yourself is fine too.

> [!TIP]
> The default stack costs about 181 dollars a month, and the Multi-AZ database is two thirds
> of it. That is the price of surviving a zone; the review prompt is a good place to ask what
> Aurora, Graviton or a Savings Plan would change.
