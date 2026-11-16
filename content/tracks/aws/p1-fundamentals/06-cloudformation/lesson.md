# Infrastructure as Code: CloudFormation and CDK

Clicking in the console does not scale: nobody can review it, repeat it in another
account, or say what changed. **AWS CloudFormation** takes a **template** (YAML or JSON)
that declares resources, and creates, updates or deletes them together as a **stack**.
It works out the order from the references between resources, rolls back on failure and
keeps the state for you, with no state file to store.

## A template

```yaml
AWSTemplateFormatVersion: "2010-09-09"
Description: Orders API network and queue

Parameters:
  Environment:
    Type: String
    AllowedValues: [dev, prod]
  VpcCidr:
    Type: String
    Default: 10.20.0.0/16

Conditions:
  IsProd: !Equals [!Ref Environment, prod]

Mappings:
  Sizes:
    dev:  { Retention: 7 }
    prod: { Retention: 400 }

Resources:
  Vpc:
    Type: AWS::EC2::VPC
    Properties:
      CidrBlock: !Ref VpcCidr
      EnableDnsSupport: true
      EnableDnsHostnames: true
      Tags: [{ Key: Name, Value: !Sub "orders-${Environment}" }]
  PublicSubnetA:
    Type: AWS::EC2::Subnet
    Properties:
      VpcId: !Ref Vpc
      AvailabilityZone: !Select [0, !GetAZs ""]
      CidrBlock: !Select [0, !Cidr [!GetAtt Vpc.CidrBlock, 6, 8]]
  OrdersQueue:
    Type: AWS::SQS::Queue
    Properties:
      MessageRetentionPeriod: 1209600
      KmsMasterKeyId: !If [IsProd, alias/orders, alias/aws/sqs]

Outputs:
  VpcId:
    Value: !Ref Vpc
    Export: { Name: !Sub "orders-${Environment}-vpc-id" }
```

The **intrinsic functions** to know: `!Ref` (a parameter's value or a resource's main id),
`!GetAtt Res.Attr` (another attribute, such as `Arn`), `!Sub` (string interpolation with
`${Name}` and pseudo parameters like `${AWS::Region}`, `${AWS::AccountId}`,
`${AWS::StackName}`), `!Join`, `!Select`, `!Split`, `!If`, `!Equals`, `!FindInMap`,
`!GetAZs`, `!Cidr` and `!ImportValue`. Dynamic references fetch values at deploy time:
`{{resolve:ssm:/path}}` and `{{resolve:secretsmanager:name:SecretString:password}}`, so no
secret ever appears in the template.

## Changing a stack safely

- A **change set** previews an update: which resources are added, modified or
  **replaced**. Some property changes (a database's identifier, a bucket's name, a
  subnet's CIDR) can only be applied by creating a new resource and deleting the old one.
  Always read the `Replacement` column.
- `DeletionPolicy: Retain` (or `Snapshot` for databases and volumes) keeps a resource when
  the stack or the resource is deleted; `UpdateReplacePolicy` does the same when it is
  replaced. Use both on everything stateful.
- **Termination protection** stops a stack from being deleted; a **stack policy** blocks
  updates to chosen resources.
- On failure the stack **rolls back** to the last good state. **Drift detection** reports
  resources someone changed by hand.
- Existing resources can be **imported** into a stack, and the IaC generator writes a
  template from what already runs.

## Organising stacks

Split by **lifecycle and owner**: a network stack that changes rarely, a data stack
(databases, buckets) with strict protection, and one stack per service. Share values with
**exports** and `!ImportValue` (an exported value cannot change while it is imported) or,
more loosely, with SSM parameters. **Nested stacks** reuse templates inside a parent;
**StackSets** deploy one template to many accounts and regions.

**AWS SAM** is a CloudFormation **transform** (`Transform: AWS::Serverless-2016-10-31`)
with short resource types for serverless apps: `AWS::Serverless::Function`, `::Api`,
`::SimpleTable`; it expands into the full resources at deploy time (Phase 2).

## The AWS CDK

The **Cloud Development Kit** lets you write infrastructure in Java, Python, TypeScript,
C# or Go. **Constructs** are classes: L1 constructs map one to one to CloudFormation
resources (`CfnBucket`), L2 constructs add sensible defaults and helper methods
(`Bucket`, `bucket.grantRead(role)` writes the least-privilege policy for you), L3
patterns build whole architectures. `cdk synth` produces a CloudFormation template,
`cdk diff` shows the change set, `cdk deploy` deploys it.

```typescript
const table = new dynamodb.TableV2(this, "Orders", {
  partitionKey: { name: "pk", type: dynamodb.AttributeType.STRING },
  removalPolicy: RemovalPolicy.RETAIN,
});
const fn = new lambda.Function(this, "Api", { /* ... */ });
table.grantReadWriteData(fn); // an exact IAM policy, no wildcards on other tables
```

Terraform and Pulumi are popular alternatives with their own state; the ideas (plan,
review, apply, protect state) are the same.

## Checking templates

- **cfn-lint** validates every resource and property against the AWS schemas and best
  practices: the dojo runs it on every template you write.
- **cfn-guard** checks templates against policy rules you write ("every bucket is
  encrypted", "no security group allows 0.0.0.0/0 on 22"); you will write rules in Phase 3.
- **cdk-nag** and **Checkov** do the same for CDK apps and many IaC formats.

## Summary

- A template declares resources; a stack creates and updates them together, with rollback.
- Parameters, conditions, mappings and intrinsic functions make one template fit many environments.
- Read change sets for replacements; Retain or Snapshot everything stateful.
- Split stacks by lifecycle; share values with exports or SSM; SAM shortens serverless templates.
- The CDK writes CloudFormation from a real language; lint every template in CI.

## Further reading

- What is AWS CloudFormation?: <https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/Welcome.html>
- Template anatomy: <https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-anatomy.html>
- Intrinsic function reference: <https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/intrinsic-function-reference.html>
- Update stacks using change sets: <https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-changesets.html>
- DeletionPolicy attribute: <https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-attribute-deletionpolicy.html>
- AWS CDK developer guide: <https://docs.aws.amazon.com/cdk/v2/guide/home.html>
- cfn-lint: <https://github.com/aws-cloudformation/cfn-lint>
