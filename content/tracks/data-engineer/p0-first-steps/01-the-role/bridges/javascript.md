---
from: javascript
status: draft
concept: "Data behind the product"
from_way: "Reading and writing JSON documents for one user at a time from an API."
native_way: "Collecting every event and record into analytical tables that answer questions about all users."
sensei_tip: "The events your frontend sends (page views, clicks) are a classic streaming source: give them an id, a timestamp and a stable schema, and data engineers will thank you."
---

| JavaScript / TypeScript | Data engineering |
| --- | --- |
| an API response for one user | a table of all users |
| analytics.track('click') | an event in a streaming pipeline |
| a Prisma model | a source table to extract from |
| node-cron job | an orchestrated batch task |
| JSON over HTTP | JSON lines, Avro or Parquet files |
