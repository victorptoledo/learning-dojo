---
from: javascript
status: draft
concept: "Distributed in-memory computing"
from_way: "Array.map and reduce over data held in one Node.js process."
native_way: "Partitioned, lazy DataFrames that Spark executes on many machines."
sensei_tip: "map and filter on arrays are narrow transformations; anything like groupBy or a join across two arrays is wide, and in Spark it means moving data over the network."
---

| JavaScript / TypeScript | Apache Spark |
| --- | --- |
| array.map / filter | narrow transformations |
| Object.groupBy(array, fn) | groupBy: a shuffle |
| a Map for lookups | a broadcast table |
| worker_threads pool | executors running tasks |
| lazy iterators / generators | lazy evaluation |
