# Apache Spark: Partitions, Lazy Plans, Shuffles and Skew

When data no longer fits one machine, **Apache Spark** splits it into **partitions** and
processes them in parallel on a cluster. It is the most used engine for large batch
transformations (on Databricks, EMR, Dataproc, Synapse, Fabric), and its ideas explain
most performance problems of distributed SQL engines too.

## Driver, executors, partitions

```text
driver (your program, builds the plan)
  ├── executor 1: task (partition 0), task (partition 3) ...
  ├── executor 2: task (partition 1), task (partition 4) ...
  └── executor 3: ...
```

- The **driver** runs your code and turns it into a plan.
- **Executors** run **tasks**; one task processes one **partition** of the data.
- Parallelism is limited by the number of partitions and executor cores. A file read
  usually makes one partition per ~128 MB.

## DataFrames and lazy evaluation

```python
clicks = spark.read.parquet("s3://lake/clicks/")            # nothing runs yet
daily = (clicks
         .filter(F.col("event_date") >= "2026-11-01")         # still nothing
         .groupBy("user_id", "event_date")
         .agg(F.count("*").alias("clicks")))
daily.write.mode("overwrite").partitionBy("event_date").parquet("s3://lake/daily/")  # action
```

**Transformations** (`filter`, `select`, `join`, `groupBy`) only build a logical plan.
**Actions** (`write`, `count`, `collect`) run it. Because Spark sees the whole plan, the
**Catalyst** optimizer can push the filter into the Parquet scan (partition pruning,
predicate pushdown) and read only the needed columns. `df.explain()` shows the plan.

Never `collect()` a large DataFrame to the driver: it brings all the data to one machine.

## Narrow and wide transformations

- **Narrow** (`filter`, `select`, `withColumn`): each output partition depends on one
  input partition. No data moves between executors.
- **Wide** (`groupBy`, `join`, `distinct`, `orderBy`): rows with the same key must meet,
  so data is redistributed across the network: a **shuffle**. Shuffles split the job into
  **stages**, write intermediate files, and are usually the most expensive part.

`spark.sql.shuffle.partitions` (200 by default) sets the partitions after a shuffle.

## Joins

| Strategy | When | Cost |
| --- | --- | --- |
| **Broadcast hash join** | one side is small (tens of MB): it is copied to every executor | no shuffle of the large side |
| **Sort-merge join** | both sides large | both sides shuffled by key and sorted |
| Shuffle hash join | medium sides | shuffle, then hash |

Spark broadcasts automatically under `spark.sql.autoBroadcastJoinThreshold`, or you hint
it: `big.join(F.broadcast(small), "country_code")`.

## Skew

A shuffle sends all rows of one key to one partition. If one key has a huge share of the
rows (a `NULL` user id, a bot, one giant customer), one task gets most of the work while
the others idle: **data skew**. The Spark UI shows it as one task taking far longer than
the median, often with **spill** (data written to disk because it does not fit in memory).

Fixes:

- Look at key frequencies first; filter or handle `NULL` and bot keys separately.
- **Adaptive Query Execution** (AQE, on by default since Spark 3.2) re-plans at runtime: it
  coalesces small shuffle partitions, switches to broadcast joins when a side turns out
  small, and **splits skewed partitions** in sort-merge joins.
- **Salting**: add a random suffix to the hot key on the large side and replicate the
  matching rows on the other side, so the hot key spreads over N partitions; aggregate
  twice (per salted key, then per key).

## Writing output

Each task writes its own file per output partition, so 200 shuffle partitions times 30
dates can mean 6,000 small files. `repartition("event_date")` before
`write.partitionBy("event_date")`, or `coalesce(n)` to reduce files, keeps the output
healthy. Prefer a table format (Delta, Iceberg) for atomic overwrites.

## Summary

- The driver plans; executors run one task per partition.
- Transformations are lazy; actions run the optimised plan; avoid `collect()` on big data.
- Wide transformations shuffle and create stages: they dominate cost.
- Broadcast small tables; use sort-merge for two large ones.
- Skew makes one task slow: check keys, rely on AQE, salt hot keys.
- Control the number of output files.

## Further reading

- Apache Spark documentation, SQL performance tuning and adaptive query execution: <https://spark.apache.org/docs/latest/sql-performance-tuning.html>
- Apache Spark documentation, RDD programming guide (shuffle operations): <https://spark.apache.org/docs/latest/rdd-programming-guide.html#shuffle-operations>
- Bill Chambers and Matei Zaharia, *Spark: The Definitive Guide* (O'Reilly), parts II and IV
- Databricks documentation, "Comprehensive guide to optimize Databricks, Spark and Delta Lake workloads": <https://www.databricks.com/discover/pages/optimize-data-workloads-guide>
- Matei Zaharia et al., "Resilient Distributed Datasets" (NSDI 2012): <https://www.usenix.org/system/files/conference/nsdi12/nsdi12-final138.pdf>
