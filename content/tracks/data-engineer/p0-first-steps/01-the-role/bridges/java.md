---
from: java
status: draft
concept: "Data work from an application developer's view"
from_way: "Writing services that read and write single rows through JPA, with the database as a detail."
native_way: "Pipelines that move and reshape whole tables between systems, with SQL as the main language."
sensei_tip: "Your JDBC and transaction knowledge carries over, but think in sets: one INSERT ... SELECT over a million rows, not a million repository.save() calls."
---

| Java | Data engineering |
| --- | --- |
| a Spring Batch job | a batch pipeline task |
| repository.findAll() in a loop | one set-based SQL statement |
| a JPA entity per table | tables modelled for reading: facts and dimensions |
| Quartz or @Scheduled | an orchestrator such as Airflow or Dagster |
| Kafka consumer in a service | a streaming pipeline |
