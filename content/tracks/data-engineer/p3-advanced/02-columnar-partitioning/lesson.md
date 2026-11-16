# Columnar Storage, Parquet Internals and Partitioning

Most analytical queries read a few columns of many rows and filter on a few of them. A
good file layout lets the engine read **only the bytes it needs**. This lesson opens a
Parquet file and then looks at how files are arranged into a table.

## Inside a Parquet file

```text
file
├── row group 0          (e.g. 128 MB of rows)
│   ├── column chunk: event_id   -> pages
│   ├── column chunk: country    -> pages
│   └── column chunk: amount     -> pages
├── row group 1
│   └── ...
└── footer: schema, row group offsets, per-chunk statistics (min, max, null count)
```

- A **row group** holds a horizontal slice of the rows; inside it each column is stored as
  a **column chunk**, split into **pages**.
- The **footer** holds the schema and the location and **statistics** of every chunk. A
  reader reads the footer first and plans from it.

## Encodings and compression

Values of one column are similar, so they encode well:

- **Dictionary encoding**: `country` with 50 distinct values becomes small integer codes
  plus a dictionary.
- **Run-length encoding** (RLE): sorted or repetitive values (`GB, GB, GB, ...`) become
  "GB times 10,000".
- **Bit packing** and delta encoding for integers and timestamps.
- Then a **compression codec** per page: Snappy (fast), Zstandard (better ratio, the common
  choice today), gzip.

Ten times smaller than the equivalent CSV is normal, and smaller means less I/O.

## Projection and predicate pushdown

- **Projection pushdown**: `SELECT amount` reads only the `amount` chunks.
- **Predicate pushdown**: `WHERE event_id BETWEEN 5000 AND 6000` skips every row group
  whose min/max statistics do not overlap the range, without decompressing it. Bloom
  filters (optional) help with equality on high-cardinality columns.

Statistics only prune well when values are **clustered**: if `event_id` is random in every
row group, every min/max range overlaps everything. **Sorting** data before writing (or
Z-ordering on several columns) is what makes statistics effective.

## Partitioning

Above files, a table is often split into **partitions**: folders by the value of a column,
the Hive layout:

```text
events/event_date=2026-11-14/part-0.parquet
events/event_date=2026-11-15/part-0.parquet
```

A query with `WHERE event_date = '2026-11-15'` never opens the other folders: **partition
pruning**, the cheapest skipping of all. It also makes loads simple: a daily job
**overwrites one partition**, which is idempotent.

Choosing the key:

- Partition on columns that **almost every query filters on**, usually a date.
- Keep **cardinality low**: thousands of partitions, not millions. Partitioning by
  `user_id` creates millions of tiny folders.
- Aim for partitions holding at least a few large files.

## The small-files problem

Each file costs: a listing call, an open, a footer read, a task in Spark. Streaming jobs and
over-partitioning create **thousands of tiny files**, and queries spend their time on
overhead. Typical targets are files of **128 MB to 1 GB**. Fixes:

- write fewer, larger files (repartition before writing, larger batches);
- **compact** regularly: rewrite a partition's small files into a few large ones, safely
  (write the new files first, then swap; table formats do this as a commit);
- partition less finely, and use sorting within files for the other columns.

## Summary

- Parquet stores row groups of column chunks with a footer of schema and statistics.
- Dictionary, RLE and compression make columns small; smaller is faster.
- Projection reads only needed columns; predicate pushdown skips row groups by statistics,
  which works when data is sorted or clustered.
- Partition by low-cardinality columns that queries filter on; overwrite partitions to stay
  idempotent.
- Avoid small files: size files sensibly and compact.

## Further reading

- Apache Parquet documentation, concepts and file format: <https://parquet.apache.org/docs/concepts/>
- Apache Arrow documentation, reading and writing Parquet with pyarrow: <https://arrow.apache.org/docs/python/parquet.html>
- DuckDB documentation, Parquet and Hive partitioning: <https://duckdb.org/docs/stable/data/partitioning/hive_partitioning>
- Apache Spark documentation, Parquet files and partition discovery: <https://spark.apache.org/docs/latest/sql-data-sources-parquet.html>
- Daniel Abadi et al., "The Design and Implementation of Modern Column-Oriented Database Systems" (Foundations and Trends in Databases, 2013)
