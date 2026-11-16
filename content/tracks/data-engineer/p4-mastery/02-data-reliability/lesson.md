# Reliable Data Products: SLOs, Observability, Incidents and Cost

Once dashboards, finance reports and models depend on your tables, pipelines are
**production systems**. The practices of site reliability engineering apply, with one
twist: data can be **wrong** while every job is green.

## Data products

Treat important datasets as **products**: they have consumers, an owner, documentation,
a contract (schema and meaning), and promised service levels. "Daily revenue for finance,
ready by 8:00" is a product; a scratch table is not. Being explicit about which datasets
are products tells you where reliability work pays off.

## Data SLOs

| Indicator (SLI) | Example objective (SLO) |
| --- | --- |
| **Freshness** | the previous day's data is published by 08:00 on 99% of days |
| **Completeness** | row count within the expected range; every source partition present |
| **Correctness** | totals reconcile with the source system within 0.1%; quality checks pass |
| **Availability** | the table is queryable |

An **error budget** (1% of days may miss) lets you balance new work and reliability, as for
services.

## Observability and alerting

Monitor what consumers experience, not only whether tasks ran:

- **Pipeline health**: task failures, durations, retries, consumer lag for streams.
- **Data health** (data observability): freshness of each important table, volume compared
  with history, schema changes, distribution shifts (null rates, averages), quality-check
  results.
- Alert on **symptoms** with owners and runbooks: "fct_revenue not updated by 07:00" or
  "yesterday's row count 40% below the 4-week median". Avoid alerts nobody acts on.

Anomaly checks on volume and distributions catch the failures rules do not foresee: a source
that silently sends half the data, a currency switching to cents.

## Incidents

1. **Detect** (ideally before consumers do).
2. **Communicate early**: tell consumers which data is affected and since when; a banner on
   the dashboard is better than a wrong number in a meeting.
3. **Mitigate**: stop the bad data from spreading (pause downstream, restore the previous
   snapshot, time travel).
4. **Fix and backfill** the affected dates, in dependency order, and verify.
5. **Postmortem**, blameless: timeline, impact, root cause, why detection took that long,
   and actions (a new check, a contract with the source team).

Lineage tells you the blast radius: every downstream table and dashboard of the bad dataset.

## Changing pipelines safely

- Everything in **version control**: SQL models, DAGs, infrastructure.
- **CI** runs unit tests and builds changed models on sample or recent data; tools such as
  dbt's `state:modified+` build only what changed and its children.
- **Environments**: development and staging schemas, never testing in production tables.
- **Compare before switching**: run the new version next to the old one and diff the
  outputs (row counts, totals, sampled rows), then switch atomically.
- Backwards-compatible changes first (add the new column, migrate consumers, then remove
  the old), as with APIs.

## Cost

Warehouses and clusters bill for what queries scan or how long compute runs. Common levers:

- **Partition pruning and clustering** so queries read less.
- **Incremental models** instead of full rebuilds of large tables.
- **Auto-suspend** idle warehouses and right-size clusters; spot instances for batch.
- Expire old snapshots and raw data past retention; tier cold data to cheaper storage.
- Show cost per team or per pipeline, with **budgets and alerts**: people optimise what
  they can see.

## Summary

- Important datasets are products with owners, contracts and SLOs.
- Define freshness, completeness and correctness SLOs, and alert on symptoms.
- Data observability watches freshness, volume, schema and distributions.
- Incidents: detect, communicate early, contain, fix and backfill, blameless postmortem.
- Version, test and compare changes; make cost visible and prune what queries read.

## Further reading

- Google, *Site Reliability Engineering*, chapter 4 "Service Level Objectives": <https://sre.google/sre-book/service-level-objectives/>
- Barr Moses, Lior Gavish and Molly Vorwerck, *Data Quality Fundamentals* (O'Reilly, 2022)
- dbt documentation, node selection and state (`state:modified+`): <https://docs.getdbt.com/reference/node-selection/methods#state>
- Google, *Site Reliability Engineering*, chapter 15 "Postmortem Culture": <https://sre.google/sre-book/postmortem-culture/>
- FinOps Foundation, FinOps framework: <https://www.finops.org/framework/>
