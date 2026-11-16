---
from: csharp
status: draft
concept: "Analytical storage"
from_way: "SQL Server with reporting databases and SSAS cubes."
native_way: "Azure Synapse, Microsoft Fabric lakehouses (Delta tables on OneLake) or Snowflake."
sensei_tip: "Microsoft Fabric stores every table as Delta Parquet in OneLake, so the warehouse, Spark notebooks and Power BI read the same files: a lakehouse by default."
---

| C# | Warehouses and lakehouses |
| --- | --- |
| a SQL Server reporting copy | an early warehouse |
| Azure Synapse dedicated pools | an MPP warehouse |
| Fabric lakehouse | Delta tables on object storage |
| ADLS Gen2 | object storage for a lake |
| Power BI Direct Lake | BI on lakehouse files |
