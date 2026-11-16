---
from: csharp
status: draft
concept: "Analytical storage on disk"
from_way: "SQL Server rowstore tables and CSV exports for analysts."
native_way: "Columnar storage (Parquet files, or SQL Server columnstore indexes) with segment elimination."
sensei_tip: "SQL Server columnstore indexes work like Parquet: row groups of about a million rows, column segments with min/max metadata, and segment elimination for filters."
---

| C# | Columnar storage |
| --- | --- |
| a rowstore table | row-oriented storage |
| CLUSTERED COLUMNSTORE INDEX | columnar storage in SQL Server |
| segment elimination | predicate pushdown |
| Parquet.Net ParquetReader | reading Parquet in .NET |
| table partitioning by date | partition pruning |
