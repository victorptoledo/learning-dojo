Routes decide where packets *can* go; now decide who may talk to whom. Security groups are
stateful (a reply is always let back in); network ACLs are stateless (every direction needs
its own rule) and apply to a whole subnet.

**Security groups**, each with a `GroupDescription` and `VpcId: !Ref Vpc`:

| Group | Ingress | Egress (nothing else) |
| --- | --- | --- |
| `AlbSecurityGroup` | tcp 443 from `0.0.0.0/0` | tcp 8080 to `AppSecurityGroup` |
| `AppSecurityGroup` | tcp 8080 from `AlbSecurityGroup` | tcp 443 to `0.0.0.0/0` (AWS APIs, through NAT or endpoints) and tcp 5432 to `DataSecurityGroup` |
| `DataSecurityGroup` | tcp 5432 from `AppSecurityGroup` | none: the databases start no connections |

Two things CloudFormation makes you think about:

- **Cycles.** `AlbSecurityGroup`'s egress names `AppSecurityGroup`, and `AppSecurityGroup`'s
  ingress names `AlbSecurityGroup`: written inline in both, each group waits for the other
  and the stack never deploys (cfn-lint refuses it). Move one side of each pair into its own
  `AWS::EC2::SecurityGroupIngress` (or `SecurityGroupEgress`) resource.
- **The default egress rule.** A group with no inline `SecurityGroupEgress` gets "all traffic
  to `0.0.0.0/0`". Listing inline rules replaces it. To allow *nothing*, list one rule that
  matches nothing useful: `IpProtocol: "-1"` to `CidrIp: 127.0.0.1/32`.

**Network ACL** `DataNetworkAcl`, associated with the three data subnets
(`AWS::EC2::SubnetNetworkAclAssociation`), with these `AWS::EC2::NetworkAclEntry` rules and
no others:

- inbound tcp 5432 from each app subnet's block;
- outbound tcp 1024–65535 (the client's ephemeral ports, for the replies) to each app subnet's
  block.

Take the blocks from the subnets (`!GetAtt AppSubnetA.CidrBlock`), never typed in. The tests
send packets through your ACL rule by rule (lowest `RuleNumber` first, first match wins,
anything unmatched denied), with both `VpcCidr` values.

> [!TIP]
> `Protocol` in an ACL entry is a number: `6` is TCP, `17` UDP, `-1` everything. The ACL
> denies by default, so it needs no deny rules at all.
