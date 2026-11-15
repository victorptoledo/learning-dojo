---
from: javascript
status: draft
concept: "Pipelines over collections"
from_way: "Array methods map, filter, reduce and Object.groupBy."
native_way: "Streams, which are lazy and single-use, ending in a terminal operation."
sensei_tip: "A stream is not an array: it runs only when a terminal operation such as toList() is called, and it can be consumed once."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| arr.filter(p).map(f) | list.stream().filter(p).map(f).toList() |
| arr.reduce((a, b) => a + b, 0) | reduce(0, Integer::sum) |
| Object.groupBy(arr, f) | collect(groupingBy(f)) |
| arr.some(p) | anyMatch(p) |
| arr.flatMap(f) | flatMap(f) |
