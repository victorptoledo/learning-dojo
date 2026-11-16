# ETL vs ELT and the Layers of a Pipeline

Every pipeline **E**xtracts data from sources, **T**ransforms it and **L**oads it. The
letters are the same in ETL and ELT; the order says **where** the transformation runs, and
that changes how you build, debug and evolve everything.

## ETL: transform, then load

```text
sources -> [ ETL server: clean, join, aggregate ] -> warehouse (final tables only)
```

ETL comes from the time when warehouse storage and compute were expensive: only the final,
cleaned tables were worth storing. The transformation lived in a dedicated tool (Informatica,
SSIS, Talend) or custom code.

- **Pros**: the warehouse stores only what is needed; sensitive fields can be removed before
  they land anywhere.
- **Cons**: the raw data is gone, so a bug in the transformation or a new question means
  extracting again from the source (if the source still has it); the logic is hidden in a
  tool, hard to test and review.

## ELT: load, then transform

```text
sources -> raw tables/files (unchanged) -> SQL in the warehouse -> modelled tables
```

Cloud warehouses (BigQuery, Snowflake, Redshift, Databricks) made storage cheap and
compute elastic. So the modern default is to **load the data as it is** and transform it
**inside** the warehouse with SQL, usually managed by a tool such as **dbt**, which turns
`SELECT` statements into tables and views, in dependency order, with tests.

- **Pros**: the raw history stays, so you can rebuild any model after a fix; transformations
  are plain SQL in git, reviewed and tested like code; analysts can add models themselves.
- **Cons**: you store more (cost, and personal data in raw tables must be protected); badly
  written models can burn a lot of compute; without discipline you get hundreds of
  overlapping tables.

Many real pipelines are a mix: a light "T" during extraction (dropping a password column,
masking card numbers) and the heavy modelling in the warehouse.

## Layers

ELT pipelines keep data in layers, each built only from the one before it:

| Layer | Also called | Contents | Rules |
| --- | --- | --- | --- |
| **raw** | landing, bronze | exactly what the source sent, plus load metadata (`_loaded_at`, file name) | append-only, never edited |
| **staging** | silver, clean | one table per source table: renamed columns, cast types, deduplicated | one-to-one with sources, no business logic |
| **marts** | gold, presentation | facts and dimensions, aggregates for a team | business logic, documented and tested |

Because each layer is derived, you can drop and **rebuild** staging and marts from raw at
any time. That property is worth protecting: it is what makes fixes cheap.

```sql
-- staging: clean one source table, no joins, no business rules
CREATE VIEW stg_orders AS
SELECT CAST(id AS integer)              AS order_id,
       lower(trim(status))              AS status,
       CAST(amount AS numeric(10, 2))   AS amount,
       CAST(created AS timestamptz)     AS ordered_at
FROM raw_shop_orders;

-- mart: business logic on top of staging
CREATE TABLE fct_daily_revenue AS
SELECT CAST(ordered_at AS date) AS day, sum(amount) AS revenue
FROM stg_orders
WHERE status <> 'cancelled'
GROUP BY 1;
```

## Reverse ETL

Sometimes the result must go **back** into operational tools: a "customer health score"
computed in the warehouse pushed into the CRM, or audiences into an ads platform. That is
called reverse ETL. The same rules apply: idempotent writes, rate limits, and a clear owner.

## Choosing

| Situation | Leaning |
| --- | --- |
| Cloud warehouse or lakehouse, analysts writing SQL | ELT |
| Personal data that must never be stored | transform (mask, drop) before load |
| Very large raw volumes that nobody will query (raw logs) | filter early, keep raw files in cheap storage |
| Logic that needs code, not SQL (parsing PDFs, calling a model) | a Python step, then load |

## Summary

- ETL transforms before loading; ELT loads raw data and transforms it in the warehouse.
- ELT keeps history, puts logic in versioned SQL and makes rebuilds possible.
- Organise data in raw, staging and mart layers; never edit raw, rebuild the rest.
- Protect personal data and watch compute cost in ELT.

## Further reading

- dbt documentation, "How we structure our dbt projects": <https://docs.getdbt.com/best-practices/how-we-structure/1-guide-overview>
- Joe Reis and Matt Housley, *Fundamentals of Data Engineering*, chapter 8 (queries, modelling and transformation)
- Databricks, "What is a medallion architecture?": <https://www.databricks.com/glossary/medallion-architecture>
- PostgreSQL documentation, `CREATE TABLE AS` and `CREATE VIEW`: <https://www.postgresql.org/docs/17/sql-createtableas.html>
