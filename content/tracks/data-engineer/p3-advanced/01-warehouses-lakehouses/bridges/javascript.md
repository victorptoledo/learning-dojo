---
from: javascript
status: draft
concept: "Analytical storage"
from_way: "Querying the application's database or an analytics SaaS API from the front end."
native_way: "A warehouse or lakehouse that dashboards and APIs query, kept apart from the app database."
sensei_tip: "Product analytics SDKs send events to a vendor; exporting them to your own warehouse or lake (a common feature) is what lets you join them with sales data."
---

| JavaScript / TypeScript | Warehouses and lakehouses |
| --- | --- |
| the app's PostgreSQL | OLTP, not for analytics |
| @google-cloud/bigquery | a warehouse client |
| events exported to S3 as JSON | a lake's bronze layer |
| DuckDB-Wasm in the browser | an engine reading Parquet |
| a dashboard API with caching | serving from gold tables |
