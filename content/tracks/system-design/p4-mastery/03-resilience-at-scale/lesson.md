# Resilience at Scale: Cells, Shuffle Sharding and Multi-Region

At a large scale, something is always broken: a disk, a host, a deploy, a zone. Resilience is
no longer about preventing failure but about **containing** it: limiting how many customers a
failure reaches (the **blast radius**), keeping the system working without the broken part, and
recovering predictably. This lesson collects the architecture patterns that big cloud
providers use for that.

## Measure first: SLIs, SLOs and error budgets

| Term | Meaning | Example |
| --- | --- | --- |
| SLI (indicator) | a measured ratio of good events | requests answered in < 300 ms with a non-5xx status, over all requests |
| SLO (objective) | the target for the SLI over a window | 99.9 % over 30 days |
| Error budget | what the SLO allows to fail | 0.1 % of requests, about 43 minutes of full outage a month |
| SLA (agreement) | a contract with penalties, looser than the SLO | 99.5 % or credits |

The error budget turns reliability into a decision rule: while budget remains, ship features;
when it is spent, slow releases and invest in reliability. Alert on the **burn rate** (how fast
the budget is being consumed), not on every blip.

## Failure domains and redundancy

| Domain | Fails because of | Typical protection |
| --- | --- | --- |
| Process / host | crashes, hardware, kernel bugs | several instances behind a load balancer |
| Availability zone (one or more datacenters) | power, cooling, network, a bad local change | spread instances and replicas over 3 zones |
| Region | large outages, regional control-plane failures, a bad global change | multi-region designs, below |
| Dependency / provider | its own outages | timeouts, fallbacks, caching, a second provider for critical paths |
| Your own deploy | bugs, bad configuration | progressive rollout, automatic rollback |

The most frequent cause of large outages is **change**: deploys and configuration pushes. Roll
out in waves (one host, one zone, one region, then the rest), watch the metrics between waves,
and roll back automatically.

## Static stability

A system is **statically stable** if it keeps working when a dependency fails, *without having to
do anything new*. The classic example: run enough capacity in three zones that losing one leaves
the other two able to carry the load (each zone at most ~66 % utilised, the "N+1 zones"), instead
of planning to launch new instances when a zone fails, which needs the control plane at exactly
the moment it is overloaded. Related rules:

- Separate the **data plane** (serving requests) from the **control plane** (creating,
  configuring): the data plane must keep working when the control plane is down.
- Prefer **constant work**: a system that does the same amount of work whatever happens (for
  example, pushing the full configuration every minute rather than only changes) has no sudden
  mode switch to fail in.
- Be careful with **fallbacks** that are rarely exercised: a fallback path that runs only during
  outages is untested code running at the worst time. Prefer making the primary path more
  reliable, or exercise the fallback constantly.

## Cells: limiting the blast radius

A **cell-based architecture** splits a service into many identical, independent copies (cells),
each with its own compute, storage and queues, each serving a subset of customers. A thin
**routing layer** maps each customer (by id) to a cell.

```text
             ┌─ cell 1: app + db + queue ── customers A–C
router ──────┼─ cell 2: app + db + queue ── customers D–F
(by tenant)  └─ cell 3: app + db + queue ── customers G–K   ← a bad deploy or poison request hits only these
```

Benefits: a failure (a bug, an overload, a poison request) affects one cell's customers; cells
have a known maximum size, so scaling means adding cells, not discovering new limits; deploys go
cell by cell. Costs: the router is critical and must be very simple; cross-cell operations
(global search, migrations of a customer between cells) need extra design.

## Shuffle sharding

With 8 workers and plain sharding into 4 shards of 2, a customer whose requests crash workers
(a poison payload) takes down its shard and every customer on it: 1/4 of everybody. With
**shuffle sharding**, each customer is assigned a random **combination** of 2 of the 8 workers:

```text
combinations of 2 out of 8 = 28
the poison customer uses workers {3, 6}; another customer fully shares its pair with probability 1/28
customers that share one worker keep working through their other one (with retries)
```

With 100 workers and 5 per customer there are about 75 million combinations, so almost no two
customers share their whole set. Amazon Route 53 and other AWS services use this to isolate
tenants on shared fleets.

## Multi-region

| Pattern | How | RTO / RPO | Cost and complexity |
| --- | --- | --- | --- |
| Backup and restore | backups copied to another region | hours / hours | low |
| Pilot light | data replicated, minimal infrastructure idle | tens of minutes / minutes | low to medium |
| Warm standby | a scaled-down full copy running | minutes / seconds to minutes | medium |
| Active-active | all regions serve traffic | near zero / near zero for some data | high: conflicts, data residency, global consistency |

**RTO** (recovery time objective) is how long you may be down; **RPO** (recovery point
objective) is how much data you may lose. Asynchronous cross-region replication means an RPO of
seconds; synchronous means hundreds of milliseconds added to every write. Active-active needs a
plan for conflicting writes (home region per record, CRDTs, or a globally consistent database) and
routing users to their region (latency DNS, anycast).

Whatever the pattern, a **failover you have not tested is a hope**: run game days and region
evacuation drills, and practise restoring backups.

## Graceful degradation

Decide in advance what to switch off under stress, with **feature flags** or load-shedding
priorities: recommendations, personalisation, non-critical writes, expensive searches. A product
page without recommendations is a success; a timeout is not.

## Learning from incidents

Every incident ends with a **blameless postmortem**: timeline, impact, root causes (plural:
the trigger and the conditions that let it spread), what went well, and action items with owners.
Track the actions; the same incident twice is the expensive one.

## Summary

- Define SLIs and SLOs; spend and protect the error budget; alert on burn rate.
- Know your failure domains; change is the main cause of outages, so roll out in waves.
- Static stability: pre-provision for the loss of a zone; keep the data plane independent of the
  control plane; prefer constant work; distrust rarely used fallbacks.
- Cells and shuffle sharding limit the blast radius of bugs, overloads and poison requests.
- Choose a multi-region pattern from the RTO and RPO you need, and test failover regularly.

## Further reading

- Amazon Builders' Library, "Static stability using Availability Zones": <https://aws.amazon.com/builders-library/static-stability-using-availability-zones/>
- Amazon Builders' Library, "Workload isolation using shuffle-sharding": <https://aws.amazon.com/builders-library/workload-isolation-using-shuffle-sharding/>
- AWS Well-Architected, "Reducing the Scope of Impact with Cell-Based Architecture": <https://docs.aws.amazon.com/wellarchitected/latest/reducing-scope-of-impact-with-cell-based-architecture/reducing-scope-of-impact-with-cell-based-architecture.html>
- AWS Well-Architected Framework, Reliability Pillar: <https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/welcome.html>
- Google SRE book, "Service Level Objectives": <https://sre.google/sre-book/service-level-objectives/> and SRE workbook, "Implementing SLOs": <https://sre.google/workbook/implementing-slos/>
- Amazon Builders' Library, "Avoiding fallback in distributed systems": <https://aws.amazon.com/builders-library/avoiding-fallback-in-distributed-systems/> and "Reliability, constant work, and a good cup of coffee": <https://aws.amazon.com/builders-library/reliability-and-constant-work/>
