---
from: csharp
status: draft
concept: "From application data to analytical data"
from_way: "Entity Framework entities and SQL Server tables that serve one application."
native_way: "Pipelines (in SQL Server terms, SSIS or Azure Data Factory jobs) that feed a warehouse built for analysis."
sensei_tip: "If you have built SSIS packages or Azure Data Factory pipelines, you have done ETL already; this track adds modelling, idempotency, quality and the modern lakehouse."
---

| C# | Data engineering |
| --- | --- |
| SSIS package | an ETL pipeline |
| SQL Server Agent job | an orchestrator schedule |
| EF Core DbContext | a source system to extract from |
| SQL Server Analysis Services cube | a star schema in a warehouse |
| Azure Data Factory | managed ingestion and orchestration |
