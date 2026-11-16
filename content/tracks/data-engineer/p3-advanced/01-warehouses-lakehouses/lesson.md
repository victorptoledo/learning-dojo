# Warehouses, Lakes and Lakehouses

Analytical data needs a home that can scan billions of rows quickly, keep years of history
cheaply, and serve many teams. Three designs compete, and most companies end up with a mix.

## Data warehouses

A **data warehouse** is a database built for analysis: BigQuery, Snowflake, Amazon
Redshift, Azure Synapse, ClickHouse. What makes them fast:

- **Columnar storage** with heavy compression: a query reads only the columns it uses.
- **MPP** (massively parallel processing): data is split across many nodes or slices, each
  scans its part, and results are combined.
- **Separation of storage and compute** (in the cloud ones): data sits in object storage,
  and compute clusters (Snowflake's virtual warehouses, BigQuery's slots) are started,
  resized and stopped independently. You pay for storage and for the compute you use.
- **Pruning metadata**: min/max values per block (micro-partitions, zone maps) let the
  engine skip data that cannot match a filter, and **clustering** keys keep related rows
  together so pruning works.

Warehouses are the easiest place for SQL analytics: managed, governed, fast. The data
lives in the vendor's format, and other engines usually read it through the vendor.

## Data lakes

A **data lake** stores files (JSON, CSV, Parquet) in cheap **object storage** (Amazon S3,
Azure Data Lake Storage, Google Cloud Storage) and lets any engine read them: Spark,
Trino, DuckDB, the warehouses themselves. It is cheap, open and holds anything, including
images and logs. Plain files lack what databases give you:

- **no transactions**: a reader can see half of a job's files; two writers can clash;
- **no reliable schema**: each file can differ;
- **slow listing**: finding the files of a table means listing folders;
- **no updates or deletes**: changing one row means rewriting files by hand.

Unmanaged, a lake becomes a **data swamp**.

## Open table formats and the lakehouse

**Table formats** add a metadata layer over Parquet files so that a folder behaves like a
database table: **Apache Iceberg**, **Delta Lake** and **Apache Hudi**.

```text
catalog -> current metadata file -> manifest list -> manifests -> data files (Parquet)
```

- **ACID commits**: a write adds new files, then atomically swaps a pointer to new
  metadata. Readers see the old snapshot or the new one, never half.
- **Snapshots and time travel**: every commit is a snapshot; you can query the table as it
  was (`FOR VERSION AS OF`, `TIMESTAMP AS OF`) and roll back.
- **Schema and partition evolution**: add, rename or drop columns by id, change the
  partitioning without rewriting old data (Iceberg's hidden partitioning).
- **Row-level changes**: `MERGE`, `UPDATE`, `DELETE` through copy-on-write (rewrite
  affected files) or merge-on-read (write delete files, merge when reading).
- **File-level statistics** in the metadata, so engines prune without listing folders.

A **lakehouse** is a lake with a table format and a catalog: open files, warehouse-like
tables, and many engines on the same data. Databricks (Delta), Snowflake, BigQuery,
Athena and Trino all read or write Iceberg now.

Table formats need maintenance: **compaction** of small files, **expiring** old snapshots
(time travel keeps old files alive and costs storage), and removing orphan files.

## The medallion layers

Lakehouses often name their layers by metal:

| Layer | Contents | Same idea as |
| --- | --- | --- |
| **Bronze** | raw data as ingested, append-only | raw |
| **Silver** | cleaned, deduplicated, conformed tables | staging |
| **Gold** | business-level aggregates and star schemas | marts |

## Choosing

| Need | Leaning |
| --- | --- |
| SQL analytics and BI for a team, little engineering | warehouse |
| Huge volumes, unstructured data, data science and ML on the same data | lakehouse |
| Several engines, open formats, avoiding lock-in | lakehouse with Iceberg or Delta |
| Cheap long-term archive | files in object storage, maybe with a table format |

## Summary

- Warehouses: columnar, MPP, separated storage and compute, managed and fast for SQL.
- Lakes: open files in object storage, cheap and flexible, but no transactions on their own.
- Table formats (Iceberg, Delta, Hudi) add ACID commits, snapshots, schema evolution and
  row-level changes to lake files: that is a lakehouse.
- Maintain table formats: compaction, snapshot expiry, orphan cleanup.

## Further reading

- Apache Iceberg documentation, table spec and reliability: <https://iceberg.apache.org/spec/>
- Delta Lake documentation: <https://docs.delta.io/>
- Apache Hudi documentation, concepts: <https://hudi.apache.org/docs/overview>
- Michael Armbrust et al., "Lakehouse: A New Generation of Open Platforms that Unify Data Warehousing and Advanced Analytics" (CIDR 2021): <https://www.cidrdb.org/cidr2021/papers/cidr2021_paper17.pdf>
- Snowflake documentation, micro-partitions and data clustering: <https://docs.snowflake.com/en/user-guide/tables-clustering-micropartitions>
