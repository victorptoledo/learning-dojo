# Lineage, Catalogs and Governance

A warehouse with a thousand tables raises questions nobody can answer by memory: where does
`revenue` in this dashboard come from? If I change this column, what breaks? Who may see
customer e-mails? Where are all copies of this person's data? **Lineage**, **catalogs** and
**governance** answer them.

## Lineage

**Lineage** is the graph of how data flows: which jobs read which datasets and write which
others.

```text
raw.shop_orders --> stg_orders --> fct_sales --> revenue_dashboard
raw.crm_customers -> stg_customers -> dim_customer --^
```

- **Table-level** lineage: dataset to dataset. **Column-level** lineage: `fct_sales.revenue`
  comes from `stg_orders.amount` times `quantity`.
- Uses: **impact analysis** before a change (what is downstream of this column?), **root
  cause** of bad numbers (walk upstream), and finding all copies of sensitive data.
- Collect it automatically, not by hand: dbt builds it from `ref()`; Spark, Airflow and
  others emit **OpenLineage** events (run, job, input and output datasets) to a backend
  such as Marquez or a catalog.

## Catalogs and ownership

A **data catalog** (DataHub, OpenMetadata, Unity Catalog, AWS Glue, Microsoft Purview) is
the search engine and inventory of datasets: schemas, descriptions, owners, lineage,
freshness, quality results, tags such as `pii`. Its value depends on two things:

- **Ownership**: every dataset has an owning team that answers questions and approves
  changes.
- **Automation**: metadata collected from the systems (schemas, lineage, query logs), so
  it stays true; descriptions written where the code lives (dbt YAML).

## Access control

Analytical data mixes sensitive and harmless columns in the same tables, so access control
must be finer than "who can query the warehouse":

- **Roles** granted to groups, not people; **least privilege**: analysts read marts, only
  pipelines write.
- **Column-level** security and **masking**: e-mail visible to support, hashed for
  analysts.
- **Row-level** security: a country manager sees their country's rows.
- Separate environments, and service accounts per pipeline, so a leaked credential has a
  small blast radius. Audit who queried what.

## Personal data and privacy

Laws such as the GDPR (EU), the LGPD (Brazil) and the CCPA (California) give people rights
over their data and require purpose, minimisation and security.

- **Classify** columns (`pii`, `sensitive`) in the catalog, ideally at ingestion.
- **Minimise**: do not ingest what nobody needs; drop or hash identifiers early when
  possible (pseudonymisation).
- **Retention**: decide how long each layer keeps data, and enforce it with partition
  expiry and snapshot expiry.
- **Erasure** ("right to be forgotten") is hard in append-only layers: you must rewrite
  files or partitions, expire old snapshots, handle Kafka retention and backups. A pattern
  that helps is **crypto-shredding**: encrypt each person's personal fields with a
  per-person key, and delete the key to make all copies unreadable.
- Keep facts with **anonymised** keys when the person is deleted, so totals stay right.

## Governance that helps

Governance fails when it is a committee that approves tables. It works when it is mostly
**defaults built into the platform**: templates that tag and mask PII, CI checks that
reject undocumented models or ungoverned columns, lineage collected automatically, and a
small set of clear policies with owners.

## Summary

- Lineage (table and column) powers impact analysis, debugging and privacy work; collect it
  automatically (dbt, OpenLineage).
- A catalog with owners and automated metadata makes data findable and trustworthy.
- Use roles, least privilege, column masks and row filters, and audit access.
- Classify, minimise and retain personal data on purpose; plan erasure across all layers.

## Further reading

- OpenLineage documentation: <https://openlineage.io/docs/>
- DataHub documentation, lineage: <https://docs.datahub.com/docs/features/feature-guides/lineage>
- PostgreSQL documentation, row security policies: <https://www.postgresql.org/docs/17/ddl-rowsecurity.html>
- Regulation (EU) 2016/679 (GDPR), articles 5 (principles) and 17 (right to erasure): <https://eur-lex.europa.eu/eli/reg/2016/679/oj>
- DAMA International, *DAMA-DMBOK: Data Management Body of Knowledge*, 2nd edition
