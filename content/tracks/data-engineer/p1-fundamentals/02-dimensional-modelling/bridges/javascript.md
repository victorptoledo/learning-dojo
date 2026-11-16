---
from: javascript
status: draft
concept: "Modelling for reading, not writing"
from_way: "Nested JSON documents shaped for one screen of the app."
native_way: "Flat fact and dimension tables that any chart can aggregate."
sensei_tip: "Charting libraries want long, flat rows (one per data point), which is exactly a fact joined to its dimensions; nested documents must be flattened first."
---

| JavaScript / TypeScript | Dimensional modelling |
| --- | --- |
| an order document with an items array | one fact row per item |
| customer embedded in each order | a shared customer dimension |
| Date objects | a date dimension with ready attributes |
| array.reduce to sum | SUM over an additive measure |
| a lookup map by id | a join on a surrogate key |
