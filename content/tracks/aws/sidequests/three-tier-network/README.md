# Sidequest: Three-tier network

A payments team is moving to AWS and needs the network first: one template, `network.yaml`,
that every environment deploys. Load balancers live in **public** subnets, the application in
**app** subnets that reach the internet only through NAT, and the databases in **data**
subnets that reach nothing at all. Three Availability Zones, so losing one zone leaves two
working. You build it in four stages:

1. the VPC and nine subnets, all carved from one CIDR block with `Fn::Cidr`, so another
   environment only changes a parameter;
2. routing: an internet gateway for the public tier, NAT for the app tier (one gateway in
   `dev`, one per zone in `prod`, chosen by a condition) and no way out for the data tier;
3. security groups chained by reference, without a dependency cycle, and a stateless network
   ACL in front of the data tier;
4. Systems Manager and S3 reached through VPC endpoints instead of a bastion host, and VPC
   flow logs with a role that only the flow logs service can assume.

How it is checked, with no AWS account:

- **cfn-lint** validates every resource against the AWS schemas (and refuses dependency
  cycles).
- The tests read the template **as a deployed stack**, once per environment: parameters and
  conditions are evaluated, resources whose condition is false disappear, and `!Select`,
  `!Cidr`, `!GetAZs`, `!GetAtt` and `!Sub` are resolved (account `111122223333`, region
  `eu-west-1` with zones `a`, `b` and `c`, stack `dojo`). They follow each subnet's routes,
  run packets through your network ACL rule by rule, and deploy the template with another
  `VpcCidr` to prove nothing is hard-coded.
- The flow logs role is resolved and run through the **dojo's IAM policy evaluator**.

With an AWS account (not needed here; NAT gateways and interface endpoints cost money every
hour, so delete the stack when you are done):

```powershell
aws cloudformation deploy --template-file network.yaml --stack-name payments-net --parameter-overrides Environment=dev --capabilities CAPABILITY_IAM
```

When you are done, `[A]` copies a review prompt: ask an AI about your network.
