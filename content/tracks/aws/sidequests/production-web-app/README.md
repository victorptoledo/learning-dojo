# Sidequest: Production web app

An online shop runs on one server in a cupboard. It is moving to AWS, and the security and
finance teams will only sign off on a stack that passes their audit: it survives losing a
zone, encrypts everything with keys the company controls, gives the application only the
permissions it uses, follows written rules that a machine can check, and says up front what
it costs. The network already exists (a VPC with public, private and data subnets in three
zones, like the one of the N2 sidequest); you write the application's stack, `app.yaml`, in
five stages:

1. an internet-facing Application Load Balancer with HTTPS only, and instances in an Auto
   Scaling group spread over the three zones, replaced one at a time;
2. PostgreSQL on RDS, Multi-AZ, encrypted with a customer managed KMS key, its password a
   secret RDS creates and rotates, snapshotted when deleted;
3. the instances' role: Session Manager, the database secret, decryption only through
   Secrets Manager, release builds and logs, and nothing else;
4. the company's rules, written by you in cfn-guard (`rules/company.guard`), that pass on
   your stack and catch each mistake in a set of bad templates;
5. cost: a cost center tag on everything, a monthly budget that alerts by email before it
   is spent, and `cost.py`, an estimate of the stack's monthly bill.

How it is checked, with no AWS account:

- **cfn-lint** validates `app.yaml` against the AWS schemas (including deprecated engine
  versions and missing dependencies).
- The tests read `app.yaml` **as a deployed stack**: they pass the parameters a real
  deployment would get (subnet ids, a certificate ARN...), resolve `!Ref`, `!GetAtt` and
  `!Sub` (account `111122223333`, region `eu-west-1`, stack `dojo`) and inspect each
  resource.
- The key policy, the instance role and the topic policy are resolved and run through the
  **dojo's IAM policy evaluator** with a matrix of requests.
- **cfn-guard** runs your rules on your stack and on fixtures, each of which breaks one rule.

With an AWS account (not needed here; this stack costs a few hundred dollars a month):

```powershell
aws cloudformation deploy --template-file app.yaml --stack-name shop --capabilities CAPABILITY_IAM --parameter-overrides VpcId=vpc-... PublicSubnetIds=subnet-a,subnet-b,subnet-c ...
cfn-guard validate --rules rules/company.guard --data app.yaml
```

When you are done, `[A]` copies a review prompt: ask an AI to review the stack against the
Well-Architected Framework.
