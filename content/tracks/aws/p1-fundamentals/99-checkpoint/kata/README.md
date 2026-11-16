# Checkpoint 1: The Orders Network

Finish `network.yaml`: the production network of the orders service in one region, in two
Availability Zones. A load balancer will sit in the public subnets, API containers in the
private subnets, and a PostgreSQL database behind them. The VPC, the internet gateway,
`PublicSubnetA` and the public route table are already there.

| Piece | Rule |
| --- | --- |
| subnets | `PublicSubnetA` 10.20.0.0/24 and `PrivateSubnetA` 10.20.16.0/20 in `!Select [0, !GetAZs ""]`; `PublicSubnetB` 10.20.1.0/24 and `PrivateSubnetB` 10.20.32.0/20 in `!Select [1, !GetAZs ""]`; all with `VpcId: !Ref Vpc` |
| public routing | both public subnets associated with `PublicRouteTable` (`0.0.0.0/0` → the internet gateway) |
| NAT | two NAT gateways, one per AZ, each in that AZ's public subnet with its own `AWS::EC2::EIP` |
| private routing | one route table per private subnet; `0.0.0.0/0` → the NAT gateway **of the same AZ**; never to the internet gateway |
| S3 | one gateway `AWS::EC2::VPCEndpoint` for `!Sub "com.amazonaws.${AWS::Region}.s3"` on both private route tables |
| `AlbSecurityGroup` | tcp 443 from `0.0.0.0/0` (already done) |
| `AppSecurityGroup` | only tcp 8080 from `AlbSecurityGroup` (fix the starter's open rules) |
| `DatabaseSecurityGroup` | only tcp 5432 from `AppSecurityGroup` |
| outputs | `VpcId`; `PublicSubnetIds` and `PrivateSubnetIds` as `!Join [",", [!Ref ...A, !Ref ...B]]` |

How the check works: cfn-lint validates every resource against the AWS schemas, the
`tests/network.guard` rules (cfn-guard) refuse admin ports open to the internet, and the
pytest tests follow each subnet's routes and each security group's sources.

With an AWS account (not needed here; NAT gateways cost money every hour):

```powershell
aws cloudformation deploy --template-file network.yaml --stack-name orders-network
aws ec2 describe-route-tables --filters Name=vpc-id,Values=<VpcId> --output table
```

There are 19 tests: 1 in `tests/test_visible.py`, 17 hidden ones and the cfn-lint check.
