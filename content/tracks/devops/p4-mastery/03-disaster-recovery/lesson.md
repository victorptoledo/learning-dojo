# Disaster Recovery and Chaos Engineering: RTO, RPO and Game Days

High availability handles the failures you expect every week: a node dies, a zone hiccups, a
process crashes. **Disaster recovery** (DR) handles the rare ones that take out much more: a
region outage, a deleted database, ransomware, a bad migration, an account compromise. And
**chaos engineering** checks, on purpose and in daylight, that both actually work.

## RTO and RPO

Two numbers per service drive every DR decision:

- **RTO** (recovery time objective): how long the service may be down after a disaster;
- **RPO** (recovery point objective): how much data, measured in time, may be lost.

```text
           last good copy        disaster                 service back
───────────────●───────────────────✖──────────────────────────●──────────►
               └──── RPO: data lost ┘└────────── RTO ──────────┘
```

They are business decisions with a cost: an RPO of zero needs synchronous replication, an RTO
of minutes needs a running second site. Classify services (tier 1: checkout, payments; tier 3:
internal reports) instead of giving everything the most expensive target.

## Recovery strategies

| Strategy | Second site | RTO | RPO | Cost |
| --- | --- | --- | --- | --- |
| **backup and restore** | nothing until needed; rebuild from IaC and restore backups | hours to a day | hours (backup interval) | lowest |
| **pilot light** | the data is replicated, core infrastructure exists but is scaled to zero | tens of minutes to hours | minutes | low |
| **warm standby** | a smaller full copy runs, ready to scale up | minutes | seconds to minutes | medium |
| **active-active** (multi-site) | both sites serve traffic | near zero | near zero (with care for conflicts) | highest, and the most complex |

Whatever the strategy, recovery depends on things people forget: the infrastructure code and
pipeline must work in the other region, the container images and secrets must be available
there, DNS (with a low TTL) or a global load balancer must switch, and third-party providers
must be reachable.

## Backups that work

- **3-2-1**: at least three copies, on two kinds of storage, one off-site; today, add **one
  immutable** copy (object lock, a vault in a separate account) that even an administrator
  cannot delete, against ransomware and compromised credentials.
- Prefer **point-in-time recovery** (continuous log shipping, such as PostgreSQL WAL archiving
  or a managed database's PITR) to nightly dumps when the RPO is short.
- Back up **configuration and state** too: Terraform state, Kubernetes objects not in Git
  (with Velero), secrets, DNS zones.
- Encrypt backups and keep the keys recoverable separately.
- **A backup you have not restored is a hope, not a backup.** Schedule automated restore tests,
  check the data, and measure how long it took: that time is your real RTO.

## Zones and regions

Most outages are local, so the first step is always **multi-zone**: replicas in at least three
availability zones, a database with a standby in another zone, load balancers across zones.
**Multi-region** protects against regional failures and some legal requirements, but doubles
much of the cost and adds data consistency problems. Go multi-region for the tier 1 services
that need it, after multi-zone is solid.

## Chaos engineering

Netflix's Chaos Monkey made the idea famous: if instances will fail anyway, make them fail
often, during working hours, so the system and the people get good at it. The **principles of
chaos engineering**:

1. define the **steady state** with metrics users care about (orders per minute, error rate);
2. form a **hypothesis**: "if one zone loses its nodes, the steady state holds";
3. inject a **real-world event**: kill Pods or nodes, add latency or packet loss, fail a
   dependency, fill a disk, expire a certificate;
4. **minimise the blast radius**: start in staging, then a small share of production traffic,
   with a clear **abort** condition and an owner watching;
5. **learn**: a broken hypothesis is a finding; fix it and run the experiment again, then
   automate it.

Tools: Chaos Mesh and LitmusChaos (Kubernetes), AWS Fault Injection Service, Azure Chaos
Studio, Gremlin, or simply `kubectl delete pod` and `tc netem` with discipline.

## Game days

A **game day** is a planned exercise of a whole scenario with the people who would respond:
"the primary region is gone", "the database was dropped", "the CI system is compromised". Run
the real runbook, time it against the RTO, and record what was missing. Tabletop versions
(talking through the scenario) cost little and find gaps in roles, access and documentation.

## Summary

- RTO limits downtime, RPO limits data loss; set them per service tier as business decisions.
- Strategies from backup and restore to active-active trade cost for faster, lossless recovery.
- Backups need 3-2-1 with an immutable copy, point-in-time recovery where RPO is short, and
  regular, measured restore tests.
- Multi-zone first; multi-region for the services that need it.
- Chaos engineering tests hypotheses about steady state with a small blast radius; game days
  rehearse whole disasters with the people who respond.

## Further reading

- Principles of Chaos Engineering: <https://principlesofchaos.org/>
- AWS, "Disaster Recovery of Workloads on AWS": <https://docs.aws.amazon.com/whitepapers/latest/disaster-recovery-workloads-on-aws/disaster-recovery-workloads-on-aws.html>
- Google Cloud, disaster recovery planning guide: <https://cloud.google.com/architecture/dr-scenarios-planning-guide>
- Velero documentation: <https://velero.io/docs/>
- *Chaos Engineering*, Casey Rosenthal and Nora Jones (O'Reilly, 2020)
