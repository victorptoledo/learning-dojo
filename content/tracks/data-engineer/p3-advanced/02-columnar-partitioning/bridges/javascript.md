---
from: javascript
status: draft
concept: "Analytical storage on disk"
from_way: "JSON arrays of objects, where every row repeats every field name."
native_way: "Columnar Parquet files, read a column at a time, with statistics to skip data."
sensei_tip: "Arrow and Parquet are the same columnar idea in memory and on disk; apache-arrow in JavaScript and DuckDB-Wasm read Parquet directly in the browser or Node."
---

| JavaScript / TypeScript | Columnar storage |
| --- | --- |
| an array of objects | row-oriented data |
| an object of typed arrays | column-oriented data |
| apache-arrow Table | columnar data in memory |
| DuckDB-Wasm read_parquet | projection and pushdown |
| gzip on the HTTP response | compression, but not splittable |
