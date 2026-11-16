---
from: python
status: draft
concept: "Analytical storage on disk"
from_way: "df.to_csv and pd.read_csv for every intermediate result."
native_way: "Parquet with pyarrow: typed, compressed columns, read selectively, partitioned by date."
sensei_tip: "pq.read_table(path, columns=[...], filters=[('event_date', '=', '2026-11-15')]) uses both projection and partition pruning; pq.ParquetFile(path).metadata shows row groups and statistics."
---

| Python | Columnar storage |
| --- | --- |
| df.to_parquet(partition_cols=['event_date']) | a Hive-partitioned write |
| pq.read_table(columns=[...]) | projection pushdown |
| filters=[...] in pyarrow | predicate and partition pushdown |
| ParquetFile(...).metadata.row_group(i) | row group statistics |
| compression='zstd' | the codec choice |
