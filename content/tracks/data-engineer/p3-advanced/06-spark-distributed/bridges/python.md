---
from: python
status: draft
concept: "Distributed in-memory computing"
from_way: "pandas DataFrames in one process, limited by the memory of the machine."
native_way: "PySpark DataFrames: lazy plans executed in parallel across a cluster, with shuffles for joins and aggregations."
sensei_tip: "PySpark looks like pandas but is lazy and distributed: avoid Python UDFs where a built-in function in pyspark.sql.functions exists (they move rows between the JVM and Python), and never toPandas() big data."
---

| Python | Apache Spark |
| --- | --- |
| pd.read_parquet | spark.read.parquet (lazy) |
| df.groupby().agg() | a wide transformation with a shuffle |
| df.merge(small) | F.broadcast(small) join |
| df.apply(python_func) | a slow Python UDF: prefer built-ins |
| df.to_parquet(partition_cols=...) | write.partitionBy(...) |
