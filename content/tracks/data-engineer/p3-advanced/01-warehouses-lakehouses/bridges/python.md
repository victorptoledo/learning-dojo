---
from: python
status: draft
concept: "Analytical storage"
from_way: "CSV and Parquet files in a folder, read with pandas, or one PostgreSQL database."
native_way: "Tables in a warehouse, or Iceberg and Delta tables on object storage read by Spark, DuckDB or Polars."
sensei_tip: "PyIceberg and the deltalake package let plain Python read and write lakehouse tables without Spark, and DuckDB reads Iceberg and Delta too."
---

| Python | Warehouses and lakehouses |
| --- | --- |
| a folder of Parquet files | a lake, without transactions |
| pyiceberg | Iceberg tables in Python |
| deltalake (delta-rs) | Delta tables in Python |
| duckdb.sql(...) on files | an engine on lake data |
| google-cloud-bigquery client | a warehouse client |
