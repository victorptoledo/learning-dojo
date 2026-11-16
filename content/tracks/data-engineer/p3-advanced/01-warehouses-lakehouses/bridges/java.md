---
from: java
status: draft
concept: "Analytical storage"
from_way: "One PostgreSQL or Oracle database for the application and the reports."
native_way: "A cloud warehouse, or Parquet files under an Iceberg or Delta table format read by several engines."
sensei_tip: "Apache Iceberg's reference implementation is Java; its Catalog and Table APIs (table.newAppend().appendFile(...).commit()) show the commit model in a few lines."
---

| Java | Warehouses and lakehouses |
| --- | --- |
| one OLTP database for everything | the reason warehouses exist |
| a JDBC connection to Snowflake or BigQuery | a warehouse client |
| Iceberg Java API commit() | an atomic snapshot |
| a database transaction | a table-format commit |
| Flyway schema migrations | table-format schema evolution |
