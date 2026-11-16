# Cloud Networking and Cost: VPCs, Load Balancers, DNS and FinOps

Every cloud gives you the same building blocks under different names, and every outage review
and every surprising bill sooner or later lands on one of them. This lesson is
provider-neutral; the **AWS** track goes into AWS's versions in depth.

| Concept | AWS | Azure | Google Cloud |
| --- | --- | --- | --- |
| private network | VPC | Virtual Network (VNet) | VPC (global) |
| firewall per instance | security group | network security group | firewall rules / policies |
| outbound internet for private hosts | NAT gateway | NAT gateway | Cloud NAT |
| layer 7 load balancer | Application Load Balancer | Application Gateway | HTTP(S) Load Balancing |
| private access to cloud services | VPC endpoints / PrivateLink | Private Endpoint | Private Service Connect |
| DNS | Route 53 | Azure DNS | Cloud DNS |

## VPCs, subnets and routes

A **VPC** is a private address space (a CIDR block such as `10.20.0.0/16`) split into
**subnets**, each in one **availability zone** (an independent data centre in the region).

```text
VPC 10.20.0.0/16
 ├─ public  10.20.0.0/24  (zone a)  ── route 0.0.0.0/0 → internet gateway   load balancer, NAT
 ├─ public  10.20.1.0/24  (zone b)
 ├─ private 10.20.10.0/23 (zone a)  ── route 0.0.0.0/0 → NAT gateway        nodes, services
 ├─ private 10.20.12.0/23 (zone b)
 └─ data    10.20.20.0/24 (zone a)  ── no route to the internet             databases
```

- A subnet is **public** only because its route table sends `0.0.0.0/0` to an internet gateway.
- **Private** subnets reach the internet (for updates, external APIs) through a **NAT
  gateway**, which allows outbound connections only.
- Spread every tier over at least two zones (three for production), and put a NAT gateway in
  each zone so one zone's failure does not cut the others off.
- Plan CIDRs so they do not overlap with other VPCs and offices you may connect later, and
  leave room: Kubernetes Pods often take an address each.

## Security rules

**Security groups** are stateful firewalls attached to instances, load balancers and
databases: allow the reply automatically, deny by default. Reference groups rather than IP
ranges: "the database group accepts 5432 from the API group". **Network ACLs** are stateless
rules on subnets, a coarse second layer. Nothing but the load balancer (and maybe a bastion,
better replaced by a session manager) should accept traffic from the internet.

## Load balancers and DNS

| Load balancer | Works on | Use for |
| --- | --- | --- |
| layer 4 (network) | TCP/UDP connections | very high throughput, static IPs, non-HTTP protocols |
| layer 7 (application) | HTTP requests | path and host routing, TLS termination, WebSockets, gRPC, WAF |

Health checks take bad targets out of rotation; connection draining (deregistration delay)
lets in-flight requests finish during deploys. In Kubernetes, a `Service` of type
`LoadBalancer` or a Gateway asks the cloud for one.

**DNS** maps names to addresses: `A`/`AAAA` records for IPs, `CNAME` for aliases (not at the
zone apex; clouds offer "alias" records there), `TXT` for verification, `MX` for mail. The
**TTL** decides how long resolvers cache an answer: lower it before a migration, raise it
afterwards. Private hosted zones resolve internal names inside the VPC. DNS-based failover and
latency or geo routing spread traffic across regions.

## Connecting networks privately

- **Private endpoints** let private subnets reach cloud services (object storage, registries,
  secrets managers) without NAT or the internet: safer and cheaper.
- **Peering** joins two VPCs; a **transit gateway** or hub-and-spoke design joins many.
- **VPN** or a dedicated link (Direct Connect, ExpressRoute, Interconnect) joins the office or
  data centre.

## How the bill is built

Cloud bills surprise teams because a few patterns dominate them:

| Driver | Why it grows | Levers |
| --- | --- | --- |
| **compute** | idle and oversized instances, nodes that never scale down | right-sizing, autoscaling, schedules for non-production, Spot or preemptible for tolerant work, savings plans or reserved capacity for the steady base |
| **storage** | snapshots, logs and old volumes nobody deletes | lifecycle rules, retention policies, cheaper storage classes |
| **data transfer** | egress to the internet, traffic between zones and regions | CDNs and caching, zone-aware routing, keeping chatty services together |
| **NAT** | per-gigabyte processing of everything private hosts download | private endpoints for cloud services, a pull-through cache for images |
| **managed services** | per-request and per-GB pricing that scales with use | understand the pricing model before adopting |
| **observability** | log volume and metric cardinality | sampling, retention tiers, dropping noisy data at the collector |

## FinOps

**FinOps** brings the DevOps idea of shared ownership to cost: engineers see and own what they
spend. The FinOps Foundation's cycle is **inform** (who spends what), **optimise** (act on it)
and **operate** (make it routine):

- **tag** every resource with owner, team, service and environment (enforced in Terraform and
  by policy), so the bill can be allocated;
- give teams **dashboards**, **budgets** and **anomaly alerts** for their own spend;
- review the biggest items regularly, compute **unit costs** (cost per order, per active user)
  so growth and waste can be told apart;
- make cost a design input: an architecture review asks "what does this cost at 10× traffic?".

## Summary

- A VPC is an address space with subnets per zone; routes make subnets public or private; NAT
  gives private hosts outbound access.
- Security groups are stateful, deny by default, and reference each other; only load
  balancers face the internet.
- Layer 4 or layer 7 load balancers with health checks and draining; DNS records and TTLs;
  private endpoints, peering and VPNs connect networks.
- Compute, storage, transfer, NAT, managed services and observability drive the bill.
- FinOps: tag, inform, budget, alert on anomalies, review unit costs with the owning teams.

## Further reading

- AWS VPC user guide: <https://docs.aws.amazon.com/vpc/latest/userguide/what-is-amazon-vpc.html>
- Google Cloud VPC overview: <https://cloud.google.com/vpc/docs/vpc>
- Azure virtual network concepts: <https://learn.microsoft.com/en-us/azure/virtual-network/concepts-and-best-practices>
- FinOps Foundation framework: <https://www.finops.org/framework/>
- *Cloud FinOps*, 2nd edition, J. R. Storment and Mike Fuller (O'Reilly, 2023)
