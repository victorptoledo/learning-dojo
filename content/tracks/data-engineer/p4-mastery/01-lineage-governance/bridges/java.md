---
from: java
status: draft
concept: "Knowing where data comes from and who may see it"
from_way: "Spring Security roles on endpoints, and knowledge of data flows kept in people's heads."
native_way: "Automatically collected lineage, a catalog with owners, and roles, masks and row filters on the data itself."
sensei_tip: "The OpenLineage Java client and Spark integration emit lineage without code changes; think of it as distributed tracing for data, where spans are job runs and datasets are the services."
---

| Java | Lineage and governance |
| --- | --- |
| @PreAuthorize on a method | access control, but on the app, not the data |
| distributed tracing spans | lineage run events |
| Maven dependency tree | lineage graph of datasets |
| a code owner file | dataset ownership |
| Jasypt or field encryption | per-field protection of PII |
