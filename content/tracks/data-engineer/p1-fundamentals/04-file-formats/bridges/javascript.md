---
from: javascript
status: draft
concept: "Serialisation formats for data"
from_way: "JSON.parse for everything, with numbers as IEEE 754 doubles."
native_way: "Typed binary formats for messages and tables, and care with numbers in JSON."
sensei_tip: "Any integer above Number.MAX_SAFE_INTEGER (2^53 - 1) is corrupted by JSON.parse: send big ids and decimal amounts as strings, or parse with a BigInt-aware parser."
---

| JavaScript / TypeScript | Data formats |
| --- | --- |
| JSON.parse | JSON lines, but numbers become doubles |
| BigInt | safe large ids |
| papaparse / csv-parse | safe CSV parsing |
| avsc | Avro in Node.js |
| parquet-wasm / DuckDB-Wasm | Parquet in JavaScript |
