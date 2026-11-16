---
from: go
status: draft
concept: "Analytical storage on disk"
from_way: "encoding/csv files written and read sequentially."
native_way: "Parquet files with typed column chunks, statistics and compression, partitioned in folders."
sensei_tip: "parquet-go (segmentio or the Apache Arrow Go module) writes Parquet from structs with tags; sort the rows by the column queries filter on before writing so statistics prune."
---

| Go | Columnar storage |
| --- | --- |
| encoding/csv Writer | row-oriented text |
| parquet-go with struct tags | writing Parquet |
| arrow/go Record | columnar data in memory |
| sort.Slice before writing | clustering for statistics |
| filepath.Join(root, "event_date="+d) | a Hive-style partition folder |
