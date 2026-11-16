---
from: javascript
status: draft
concept: "Where transformation runs"
from_way: "A Node script fetches JSON from an API, maps and filters it, and inserts the result."
native_way: "Store the JSON as it came, then shape it with SQL inside the warehouse."
sensei_tip: "Warehouses read JSON well (PostgreSQL jsonb, BigQuery JSON, Snowflake VARIANT): land the payload unchanged and extract fields in a staging model."
---

| JavaScript / TypeScript | ETL / ELT |
| --- | --- |
| fetch() from an API | extract |
| array.map / filter / reduce | the T of ETL |
| insert the mapped rows | load of ETL |
| insert the raw JSON payload | the raw layer of ELT |
| payload->>'status' in SQL | a staging model |
