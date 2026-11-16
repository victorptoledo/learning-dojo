# Leading a Data Platform

Senior data engineers spend less time writing pipelines and more time deciding **which**
pipelines, platforms and practices the organisation needs, and getting people to agree.
This lesson collects the patterns behind those decisions.

## A reference architecture

```text
sources -> ingestion -> storage -> transformation -> serving -> consumers
(SaaS,     (managed     (warehouse  (versioned SQL,    (BI, APIs,  (analysts, finance,
 DBs,       connectors,  or lake-    tests, CI)         reverse     ML, products)
 events)    CDC, streams) house)                        ETL)
              orchestration, catalog and lineage, quality, access control, monitoring
```

Most companies do not need to invent any box. The design work is choosing tools that fit
the team's size and skills, the volumes and latencies, and the governance needs, and making
the boxes work together with clear ownership.

## Build versus buy

- **Buy** (or use managed open source) what is undifferentiated and maintenance-heavy:
  connectors to SaaS APIs, the warehouse engine, orchestration hosting, BI.
- **Build** what is specific to the business: the models, the metrics, the domain events,
  internal tools that encode your conventions.
- Count the **total cost**: licences, but also engineers to operate, upgrade and on-call.
  A small team running self-hosted Kafka, Spark and Airflow spends its time on
  infrastructure instead of data.
- Prefer **open formats and interfaces** (Parquet, Iceberg, SQL, OpenLineage) to keep the
  option to change vendors.

## Organising data work

| Model | How | Works when | Risk |
| --- | --- | --- | --- |
| **Centralised** | one data team builds everything | small company, few domains | the team becomes a bottleneck far from domain knowledge |
| **Hub and spoke** | a platform team plus embedded engineers or analytics engineers in domains | growing company | needs strong shared standards |
| **Data mesh** | domains own their data as products on a self-serve platform, with federated governance | many mature domain teams | without a good platform and standards, chaos and duplication |

**Data mesh** (Zhamak Dehghani) rests on four principles: domain ownership, data as a
product, a self-serve data platform, and federated computational governance. It is an
organisational change first; a platform team's job is to make "the right way" the easy way.

## Contracts across teams

The biggest source of breakage is upstream change. Agree **data contracts** with source
teams (schema, meaning, quality, change process), prefer **domain events** through an
outbox over CDC of internal tables when teams are independent, and check contracts in the
producers' CI.

## Design documents and reviews

Write a short **design document** before significant work:

1. Context and problem, with who is affected.
2. **Goals and non-goals**.
3. The proposal, with a diagram.
4. **Alternatives considered** and why not.
5. Risks, cost, migration and rollback, operations (SLOs, on-call).
6. Milestones.

Reviews are for finding problems early, not approving: invite the consumers and the
operators, look for data quality, privacy, failure and backfill stories, and record the
decisions (an architecture decision record keeps the "why").

## Roadmaps and people

- Balance **new capabilities**, **reliability** (tests, SLOs, incident follow-ups) and
  **cost**, and say explicitly what you will not do.
- Deliver value in **thin slices**: one important data product end to end beats a
  half-built platform.
- Grow people: pair on reviews, write down conventions, rotate on-call with good runbooks,
  and let analysts contribute models with review instead of filing tickets.

## Summary

- The platform's boxes are well known; the skill is choosing, integrating and owning them.
- Buy the undifferentiated, build the business-specific, and keep formats open.
- Choose an organisational model that matches the company's maturity; data mesh needs a
  platform and governance first.
- Contracts with producers prevent most breakage.
- Design documents with goals, non-goals and alternatives make decisions explicit.

## Further reading

- Zhamak Dehghani, *Data Mesh* (O'Reilly, 2022), and "Data Mesh Principles and Logical Architecture": <https://martinfowler.com/articles/data-mesh-principles.html>
- Joe Reis and Matt Housley, *Fundamentals of Data Engineering*, chapters 3 (architecture) and 4 (choosing technologies)
- Michael Nygard, "Documenting Architecture Decisions": <https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions>
- Will Larson, *Staff Engineer: Leadership beyond the management track* (2021)
