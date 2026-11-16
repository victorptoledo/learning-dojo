---
from: csharp
status: draft
concept: "Running data in production"
from_way: "Application Insights alerts and Azure Monitor for services."
native_way: "Freshness and quality SLOs per data product, monitored with the same alerting stack."
sensei_tip: "Azure Data Factory and Fabric emit run metrics to Azure Monitor; add custom metrics for freshness and row counts so alerts reflect what consumers feel."
---

| C# | Data reliability |
| --- | --- |
| Application Insights availability test | a freshness check |
| Azure Monitor alert rule | an alert on a data SLI |
| deployment slots swap | switching to a new table version |
| Azure Cost Management budgets | cost controls |
| incident in Azure DevOps | a data incident record |
