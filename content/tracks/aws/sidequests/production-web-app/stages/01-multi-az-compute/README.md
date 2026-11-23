Start with the part users touch. The starter has the `VpcId` parameter and an empty load
balancer security group.

**Parameters** (a real deployment passes them; the tests pass made-up ids):
`PublicSubnetIds` and `PrivateSubnetIds` (`List<AWS::EC2::Subnet::Id>`, three each),
`CertificateArn` (an ACM certificate ARN, checked by an `AllowedPattern` that starts with
`^arn:aws:acm:`), `ImageId` (`AWS::SSM::Parameter::Value<AWS::EC2::Image::Id>`, default
`/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64`, so every launch gets
the latest Amazon Linux without a hard-coded AMI id) and `InstanceType` (`AllowedValues`
`t4g.small`, `t4g.medium`, `m7g.large`; default `t4g.small`).

| Resource | Rule |
| --- | --- |
| `LoadBalancerSecurityGroup` | tcp 443 and tcp 80 from `0.0.0.0/0`, nothing else |
| `AppSecurityGroup` | tcp 8080 from `LoadBalancerSecurityGroup` only |
| `LoadBalancer` | an `application` load balancer, `internet-facing`, in the public subnets, with deletion protection and `routing.http.drop_invalid_header_fields.enabled` on |
| `TargetGroup` | HTTP on 8080, `instance` targets; health check on `/healthz` every 10 seconds at most, expecting `200`; `deregistration_delay.timeout_seconds` 30 (rolling updates do not wait the default five minutes) |
| `HttpsListener` | HTTPS on 443 with your certificate and a TLS 1.3 policy (`ELBSecurityPolicy-TLS13-1-2-2021-06`), forwarding to the target group |
| `HttpListener` | HTTP on 80, answering every request with a `HTTP_301` redirect to HTTPS on 443 |
| `LaunchTemplate` | the image and type from the parameters, `AppSecurityGroup`, **IMDSv2 only** (`HttpTokens: required`, hop limit 1), and an encrypted `gp3` root volume (`/dev/xvda`) of 20 GiB |
| `AutoScalingGroup` | in the three private subnets; `MinSize` 3 (one per zone) and `MaxSize` 9; the launch template at `!GetAtt LaunchTemplate.LatestVersionNumber`; registered in the target group with `HealthCheckType: ELB` (an instance whose app does not answer is replaced) and a grace period; an `UpdatePolicy` that replaces one instance at a time and keeps at least 2 in service |
| `CpuScaling` | target tracking on `ASGAverageCPUUtilization` at 50% |
| output | `SiteUrl`: `!Sub "https://${LoadBalancer.DNSName}"` |

> [!TIP]
> `MinSize`, `MaxSize` and `DesiredCapacity` of an Auto Scaling group are strings in
> CloudFormation's schema: `MinSize: "3"`.
