---
from: csharp
status: draft
concept: "A private network you design: routes decide who can reach whom"
from_way: "Windows Firewall rules and VLANs separate the web, app and SQL Server tiers."
native_way: "A VPC with public, private app and private data subnets per AZ, and security groups chained by reference."
sensei_tip: "Model the tiers as a CDK `Vpc` with `SubnetConfiguration` (PUBLIC, PRIVATE_WITH_EGRESS, PRIVATE_ISOLATED) in C#; it writes the routes and NAT gateways for you."
---

| .NET world | AWS |
| --- | --- |
| VLANs | subnets in each AZ |
| Windows Firewall rules | security groups |
| a site-to-site link to the office | Site-to-Site VPN or Direct Connect |
| SQL Server on a private VLAN | RDS in isolated subnets |
