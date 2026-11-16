# VPC: Public Subnets, Private Subnets and NAT Gateways

A **VPC** (Virtual Private Cloud) is your own isolated network in one region. EC2
instances, RDS databases, ECS tasks, Lambda functions attached to a VPC and load
balancers all get their private IP addresses from it. The goal of this lesson: keep
databases and internal APIs away from the public internet, and know exactly which path
every packet takes.

## Address plan

A VPC has a primary IPv4 **CIDR block**, from `/16` (65,536 addresses) to `/28`, normally
from the private ranges `10.0.0.0/8`, `172.16.0.0/12` or `192.168.0.0/16`. Pick one that
does **not overlap** with your offices, other VPCs or partners: you cannot route between
overlapping networks, and changing a VPC CIDR later is painful.

A **subnet** is a slice of the VPC CIDR in **one Availability Zone**. AWS reserves five
addresses in every subnet (network, router, DNS, future use, broadcast), so a `/24` has
251 usable addresses. A common plan for `10.20.0.0/16`:

| Subnet | AZ a | AZ b | AZ c |
| --- | --- | --- | --- |
| public (load balancers, NAT gateways) | 10.20.0.0/24 | 10.20.1.0/24 | 10.20.2.0/24 |
| private app (instances, containers, Lambda) | 10.20.16.0/20 | 10.20.32.0/20 | 10.20.48.0/20 |
| private data (RDS, ElastiCache) | 10.20.100.0/24 | 10.20.101.0/24 | 10.20.102.0/24 |

Give the app tier room: containers and Lambda functions each take an IP. IPv6 can be added
as a second, globally routable block.

## Route tables and gateways

A subnet is **public** or **private** only because of its **route table**:

| Route table | Routes | Effect |
| --- | --- | --- |
| public | `10.20.0.0/16 → local`, `0.0.0.0/0 → igw-…` | instances with public IPs reach and are reachable from the internet |
| private app | `10.20.0.0/16 → local`, `0.0.0.0/0 → nat-…` (the NAT in the same AZ) | outbound only: updates, external APIs |
| private data | `10.20.0.0/16 → local` | no internet path at all |

- The **internet gateway** (IGW) is attached to the VPC and translates public IPs.
- A **NAT gateway** sits in a public subnet with an Elastic IP and lets private subnets
  start outbound connections; nothing can connect inbound through it. It is zonal: run
  one per AZ for availability, and remember it is billed per hour **and per GB**.
- An **egress-only internet gateway** does the same for IPv6.

## Security groups and network ACLs

| | Security group | Network ACL |
| --- | --- | --- |
| Attached to | an ENI (instance, task, database, load balancer) | a subnet |
| Rules | allow only | allow and deny, numbered, first match wins |
| State | **stateful**: replies are allowed automatically | **stateless**: allow the return traffic (ephemeral ports) too |
| Sources | CIDRs, prefix lists **or other security groups** | CIDRs only |

Chain security groups by **reference** instead of IP ranges: the database group allows
`5432` from the *API security group*, the API group allows `8080` from the *load balancer
group*, and only the load balancer group allows `443` from `0.0.0.0/0`. When the API scales
from 2 to 40 tasks, nothing changes. Never open `22` or `3389` to the world; use Session
Manager. Leave network ACLs at their default (allow all) unless you need a coarse subnet
deny list.

```yaml
DatabaseSecurityGroup:
  Type: AWS::EC2::SecurityGroup
  Properties:
    GroupDescription: PostgreSQL from the API only
    VpcId: !Ref Vpc
    SecurityGroupIngress:
      - IpProtocol: tcp
        FromPort: 5432
        ToPort: 5432
        SourceSecurityGroupId: !Ref ApiSecurityGroup
```

## Reaching AWS services privately

Without help, a private instance calls S3 or DynamoDB through the NAT gateway: it works,
but you pay per GB and the traffic leaves the VPC. **VPC endpoints** fix that:

- **Gateway endpoints** (S3 and DynamoDB only): a route-table entry, free.
- **Interface endpoints** (PrivateLink: SQS, Secrets Manager, ECR, STS and most others):
  an ENI in your subnets with a private DNS name; billed per hour and per GB.

Endpoint policies and the `aws:SourceVpce` condition in bucket policies restrict which
endpoints may be used. To connect VPCs: **VPC peering** (one to one, not transitive),
**Transit Gateway** (a hub for many VPCs and on-premises links), **Site-to-Site VPN** and
**Direct Connect** to your data centre.

**VPC Flow Logs** record accepted and rejected connections to CloudWatch Logs or S3: the
first thing to read when "the connection times out".

## Summary

- Plan a non-overlapping CIDR; subnets live in one AZ; use at least two AZs per tier.
- Public means "routes to an internet gateway"; private app subnets go out through NAT.
- Security groups are stateful allow lists; chain them by reference; NACLs are stateless.
- Gateway endpoints for S3 and DynamoDB, interface endpoints for the rest; Flow Logs to debug.

## Further reading

- How Amazon VPC works: <https://docs.aws.amazon.com/vpc/latest/userguide/how-it-works.html>
- Subnets for your VPC: <https://docs.aws.amazon.com/vpc/latest/userguide/configure-subnets.html>
- NAT gateways: <https://docs.aws.amazon.com/vpc/latest/userguide/vpc-nat-gateway.html>
- Security groups: <https://docs.aws.amazon.com/vpc/latest/userguide/vpc-security-groups.html>
- Network ACLs: <https://docs.aws.amazon.com/vpc/latest/userguide/vpc-network-acls.html>
- VPC endpoints (AWS PrivateLink): <https://docs.aws.amazon.com/vpc/latest/privatelink/concepts.html>
- VPC Flow Logs: <https://docs.aws.amazon.com/vpc/latest/userguide/flow-logs.html>
