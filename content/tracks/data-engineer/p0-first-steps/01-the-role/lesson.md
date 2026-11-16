# The Data Engineer Role and the Data Lifecycle

Every company runs on data that was created somewhere else: an order in the shop database,
a payment in a provider's API, a click in a mobile app. Analysts, finance, product teams
and machine learning models need that data **together, clean, on time and trustworthy**.
A data engineer builds and runs the systems that make that true.

## What the role owns

| Role | Main question | Typical output |
| --- | --- | --- |
| **Data engineer** | How does the right data get to the right place, reliably? | pipelines, warehouse and lake tables, orchestration, data platform |
| Analytics engineer | How do we model the business in clean tables? | tested SQL models (often dbt), metrics definitions |
| Data analyst | What happened, and why? | queries, dashboards, reports |
| Data scientist / ML engineer | What will happen, and what should we do? | models, features, experiments |

The lines blur: in a small company one person does all four. What marks the data
engineering part is **ownership of the flow**: if yesterday's revenue is missing from the
dashboard at 8:00, it is your pipeline that should have caught it.

## The data lifecycle

```text
generation -> ingestion -> storage -> transformation -> serving
 (sources)    (extract)    (lake,     (clean, join,     (dashboards,
                           warehouse)  model, test)      ML, reverse ETL)
```

- **Generation**: source systems you usually do not own: application databases, SaaS
  APIs, event streams, files from partners. Their schemas change without asking you.
- **Ingestion**: copying data out, as **batches** (every hour, every night) or as a
  **stream** (each event within seconds). Extracts must not hurt the source.
- **Storage**: a **data lake** (files such as Parquet in object storage), a **data
  warehouse** (an analytical database: BigQuery, Snowflake, Redshift), or a lakehouse that
  combines both.
- **Transformation**: cleaning, deduplicating, joining and modelling into tables people
  understand, such as a star schema.
- **Serving**: dashboards, ad hoc SQL, features for models, and data pushed back into
  operational tools.

Under all of it run the **undercurrents**: security and access control, data management
(quality, lineage, governance), orchestration, and software engineering practice
(version control, tests, code review, CI).

## OLTP and OLAP

| | OLTP (operational) | OLAP (analytical) |
| --- | --- | --- |
| Workload | many small reads and writes of single rows | few large scans and aggregates |
| Example | "insert order 1001", "get customer 7" | "revenue by country and month for 3 years" |
| Schema | normalised, built for correct writes | denormalised (star schemas), built for reads |
| Storage | row-oriented | usually column-oriented |

Running heavy analytical queries on the production database slows the application down
and still gives poor answers, because the history and the other sources are not there.
That is the first reason analytical stores exist.

## Batch and streaming

**Batch** processes a bounded set of data (yesterday's orders) on a schedule. It is simple,
cheap and easy to rerun. **Streaming** processes unbounded data continuously, with latency
in seconds; it is needed for fraud detection, live operations or user-facing features.
Most companies are mostly batch with a few streams. Choose by the **freshness the consumer
really needs**, not by fashion.

## The toolbox of this track

| Tool | Used for | Learn it in |
| --- | --- | --- |
| **SQL** | transformations, modelling, quality checks: the language of the warehouse | the SQL & Relational Databases track |
| **Python** | extraction, glue code, orchestration, data quality frameworks, PySpark | the Modern Python track |
| **Docker** | running databases and tools locally, packaging pipelines | the Docker & Containers track |
| Orchestrator | scheduling and retrying DAGs of tasks (Airflow, Dagster, Prefect) | this track, phase 2 |
| Git and CI | versioning pipelines and SQL models like any other code | the Git & GitHub track |

This track does not repeat those tracks: it uses them. The katas run SQL on PostgreSQL 17
and Python 3.15 in Docker, with SQLite, pyarrow and DuckDB where they fit.

## Summary

- A data engineer owns the flow of data from sources to the people and systems using it.
- The lifecycle: generation, ingestion, storage, transformation, serving, with security,
  data management and orchestration underneath.
- OLTP systems are built for transactions, OLAP stores for analysis; keep them apart.
- Batch is the default; stream when the consumer needs it.

## Further reading

- Joe Reis and Matt Housley, *Fundamentals of Data Engineering* (O'Reilly, 2022), chapters 1 to 3 on the role and the lifecycle
- Martin Kleppmann, *Designing Data-Intensive Applications* (O'Reilly), chapter 3 on OLTP, OLAP and column storage
- roadmap.sh Data Engineer roadmap (topic map): <https://roadmap.sh/data-engineer>
- PostgreSQL documentation, "The SQL Language": <https://www.postgresql.org/docs/17/sql.html>
