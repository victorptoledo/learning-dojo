# High Availability and Disaster Recovery: ELB, Route 53, CloudFront and Backups

"Everything fails, all the time" (Werner Vogels). Instances die, AZs lose power, a
deployment corrupts data, a region has a bad day. **High availability** keeps the service
up through the small failures; **disaster recovery** brings it back after the big ones.

## Two numbers first

- **RTO** (recovery time objective): how long the service may be down.
- **RPO** (recovery point objective): how much data, in time, you may lose.

They come from the business, and they drive the cost of everything else.

## Multi-AZ: the default for production

| Tier | Multi-AZ by |
| --- | --- |
| entry | an **Application Load Balancer** (or NLB) with nodes in every AZ |
| compute | an Auto Scaling group or ECS service spread over two or three AZs, replaced on failed health checks |
| egress | one NAT gateway **per AZ**, each private subnet routed to its own |
| data | RDS Multi-AZ or Aurora replicas in other AZs; DynamoDB and S3 are multi-AZ by design |
| state | sessions and caches outside the instances (ElastiCache, DynamoDB) |

Plan **static stability**: with one AZ gone, the remaining ones must carry the load
without having to launch anything (over-provision by one AZ's share, or accept degraded
service). Watch for hidden zonal dependencies: an EBS volume, a single NAT gateway, an
instance-store cache.

## Load balancers

| | ALB (layer 7) | NLB (layer 4) |
| --- | --- | --- |
| Traffic | HTTP, HTTPS, gRPC, WebSockets | TCP, UDP, TLS |
| Routing | host, path, header, query string rules; weighted target groups | by port; static IPs per AZ; preserves the client IP |
| Extras | WAF, Cognito/OIDC authentication, Lambda targets | PrivateLink endpoint services, millions of requests per second |

Health checks remove failed targets; **deregistration delay** lets in-flight requests
finish; **slow start** warms new targets. Cross-zone load balancing spreads traffic evenly
across AZs.

## Route 53, CloudFront and Global Accelerator

**Route 53** is AWS's DNS (with a 100 % availability SLA) and health checker. Routing
policies:

| Policy | Use |
| --- | --- |
| simple | one answer |
| **failover** | primary and secondary, driven by health checks |
| **latency** | send users to the region with the lowest latency |
| **weighted** | split traffic (10 % to a new stack, blue/green) |
| geolocation, geoproximity | by user location: data residency, local content |
| multivalue | several healthy answers |

**Alias records** point a name at AWS resources (ALB, CloudFront, S3 website) with no
charge for queries. **Application Recovery Controller** adds readiness checks and routing
controls for deliberate, safe regional failover.

**CloudFront** caches content in hundreds of edge locations, terminates TLS close to
users, shields origins (origin access control for S3, origin failover between two origins)
and hosts WAF. **Global Accelerator** gives two static anycast IPs that route over AWS's
network to the healthiest regional endpoint, for non-HTTP or very fast failover.

## Disaster recovery strategies

| Strategy | Second region holds | RTO / RPO | Cost |
| --- | --- | --- | --- |
| **Backup and restore** | backups and templates | hours / hours | lowest |
| **Pilot light** | data replicated continuously; core infrastructure off or minimal | tens of minutes / minutes | low |
| **Warm standby** | a scaled-down but running copy of the whole stack | minutes / seconds to minutes | medium |
| **Multi-site active-active** | full capacity, serving traffic | near zero / near zero | highest; needs conflict handling for writes |

Data replication tools: **Aurora Global Database** (storage-level replication, typically
under a second of lag, promotion in minutes), DynamoDB **global tables**, S3
**cross-region replication**, ECR replication, EBS and RDS snapshot copies. Keep the
infrastructure as code so the second region can be built or scaled from templates.

## Backups

**AWS Backup** applies backup plans (schedule, retention, lifecycle to cold storage) by tag
across EC2, EBS, RDS, Aurora, DynamoDB, EFS, S3 and more, with **cross-account and
cross-region copies** and **vault lock** (backups nobody can delete early, a defence
against ransomware). A backup you have not restored is a hope, not a backup: schedule
**restore tests** and measure them against the RTO.

## Testing failure

**AWS Fault Injection Service** (FIS) runs controlled experiments: stop instances, add
latency, throttle APIs, cut an AZ's network, with stop conditions on alarms. Game days
with these experiments find the single points of failure before customers do.

## Summary

- RTO and RPO come first; they decide architecture and cost.
- Production is multi-AZ at every tier, and statically stable when an AZ is lost.
- ALB for HTTP, NLB for TCP/UDP and static IPs; Route 53 policies and CloudFront route and shield.
- Four DR strategies, from backup and restore to active-active; replicate data to match the RPO.
- AWS Backup with cross-account copies and vault lock; test restores and inject faults.

## Further reading

- Reliability pillar, Well-Architected Framework: <https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/welcome.html>
- Disaster recovery of workloads on AWS (whitepaper): <https://docs.aws.amazon.com/whitepapers/latest/disaster-recovery-workloads-on-aws/disaster-recovery-workloads-on-aws.html>
- Elastic Load Balancing user guide: <https://docs.aws.amazon.com/elasticloadbalancing/latest/userguide/what-is-load-balancing.html>
- Route 53 routing policies: <https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/routing-policy.html>
- Amazon CloudFront developer guide: <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Introduction.html>
- AWS Backup developer guide: <https://docs.aws.amazon.com/aws-backup/latest/devguide/whatisbackup.html>
- AWS Fault Injection Service: <https://docs.aws.amazon.com/fis/latest/userguide/what-is.html>
