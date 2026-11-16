---
from: java
status: draft
concept: "Where transformation runs"
from_way: "Spring Batch readers, processors and writers that transform records in the JVM before writing them."
native_way: "Load raw data straight into the warehouse and transform it there with SQL, often managed by dbt."
sensei_tip: "An ItemProcessor is the T of ETL. In ELT that logic becomes a SELECT in the warehouse, which scales with the warehouse instead of your heap."
---

| Java | ETL / ELT |
| --- | --- |
| ItemReader | extract |
| ItemProcessor | the T of ETL, in the JVM |
| ItemWriter to final tables | load of ETL |
| writer to raw tables, then SQL | ELT |
| Flyway migrations | versioned SQL models, like dbt |
