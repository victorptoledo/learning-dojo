Operators still need a shell on the instances, and auditors want a record of every
connection. No bastion host and no port 22: Systems Manager Session Manager reaches the
instances through VPC endpoints, and VPC flow logs record the traffic.

**Private access**

| Resource | Rule |
| --- | --- |
| `EndpointSecurityGroup` | ingress only tcp 443 from `AppSecurityGroup` |
| `SsmEndpoint`, `SsmMessagesEndpoint`, `Ec2MessagesEndpoint` | `VpcEndpointType: Interface` for `com.amazonaws.${AWS::Region}.ssm`, `.ssmmessages` and `.ec2messages` (`!Sub`), `PrivateDnsEnabled: true`, `SecurityGroupIds: [EndpointSecurityGroup]`; in the app subnets of **all three** zones in `prod`, and only `AppSubnetA` in `dev` (an interface endpoint is billed per zone and hour) |
| `S3Endpoint` | a `Gateway` endpoint for `com.amazonaws.${AWS::Region}.s3` on the three app route tables and the data route table (free, and S3 traffic stops paying for NAT) |
| no admin ports | no security group anywhere allows tcp 22 or 3389, from any source |

**Flow logs**

| Resource | Rule |
| --- | --- |
| `FlowLogGroup` | `AWS::Logs::LogGroup` with `RetentionInDays` 90 in `prod` and 14 in `dev` |
| `FlowLogRole` | trusted only by `vpc-flow-logs.amazonaws.com`, and only for this account: `aws:SourceAccount` = `${AWS::AccountId}` and `aws:SourceArn` like `arn:aws:ec2:${AWS::Region}:${AWS::AccountId}:vpc-flow-log/*` (the confused deputy again); an inline policy with `logs:CreateLogStream`, `logs:PutLogEvents` and `logs:DescribeLogStreams` on `!GetAtt FlowLogGroup.Arn` only |
| `VpcFlowLog` | `AWS::EC2::FlowLog` on the VPC (`ResourceType: VPC`), `TrafficType: ALL`, `LogDestinationType: cloud-watch-logs`, `LogGroupName: !Ref FlowLogGroup`, `DeliverLogsPermissionArn: !GetAtt FlowLogRole.Arn`, `MaxAggregationInterval: 60` |

The tests resolve the role's trust policy and inline policy and ask the IAM evaluator:
flow logs of this account may assume the role, flow logs on behalf of another account may
not, EC2 may not; the role may write streams of its own log group, but not of another group,
and may not create or delete log groups.

> [!TIP]
> `!GetAtt FlowLogGroup.Arn` ends in `:*`, so it covers the group's log streams
> (`...:log-group:NAME:log-stream:eni-0abc-all`) as well as the group.
