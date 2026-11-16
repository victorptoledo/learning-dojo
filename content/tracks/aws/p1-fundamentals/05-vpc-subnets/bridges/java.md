---
from: java
status: draft
concept: "A private network you design: routes decide who can reach whom"
from_way: "An app server and its database sit in a DMZ and an internal VLAN, with a firewall team managing the rules."
native_way: "Subnets in a VPC, route tables, NAT gateways and security groups chained by reference express the same tiers in code."
sensei_tip: "JDBC timeouts to RDS are almost always security groups or routes: check that the DB group allows 5432 from the app's group, then read VPC Flow Logs."
---

| Java world | AWS |
| --- | --- |
| DMZ and internal VLAN | public and private subnets |
| firewall rules by IP | security groups referencing each other |
| an HTTP proxy for outbound calls | a NAT gateway |
| `SocketTimeoutException` | a missing route or security group rule |
