---
from: java
status: draft
concept: "Analytical storage on disk"
from_way: "Exporting and reading gigabyte-sized CSV dumps line by line."
native_way: "Parquet stores data by column with min/max statistics in its footer, so readers skip irrelevant data."
sensei_tip: "An analytical query like SELECT AVG(salary) on Parquet reads a single column, often far faster than on CSV; in Spark or parquet-java, filter pushdown happens automatically when the filter is on a raw column."
---

| Java | Columnar storage |
| --- | --- |
| BufferedReader over a CSV | row-oriented text |
| ParquetReader with a projection | projection pushdown |
| FilterApi.gt(...) in parquet-java | predicate pushdown |
| sorting a List before writing | clustering for better statistics |
| GZIPOutputStream | a compression codec |
