# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 8 right.
2. **The integration kata** (press **Enter**): write the production network of the orders
   service as a CloudFormation template: two AZs, public and private subnets, a NAT
   gateway per AZ, an S3 gateway endpoint, and security groups chained tier by tier.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| IAM | What wins, an allow or a deny? What is a trust policy? Which ARN does `s3:ListBucket` act on? |
| EC2 | What survives a stop? Why IMDSv2? What does an Auto Scaling group replace? |
| S3 | How do you serve private objects? What does versioning change about deletes? Which storage class for archives? |
| VPC | What makes a subnet public? Which way does a NAT gateway work? Stateful or stateless? |
| CloudFormation | `!Ref` or `!GetAtt`? What does Replacement mean in a change set? How do you keep data on delete? |

> [!TIP]
> The kata's tests follow packets: for every private subnet they walk association → route
> table → default route → NAT gateway → its subnet and AZ. Draw the network on paper first.
