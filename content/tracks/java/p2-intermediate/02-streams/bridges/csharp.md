---
from: csharp
status: draft
concept: "Pipelines over collections"
from_way: "LINQ: Where, Select, GroupBy, Sum, ToList, with deferred execution."
native_way: "Streams: filter, map, collect(groupingBy), sum, toList."
sensei_tip: "LINQ to streams is mostly renaming: Where is filter, Select is map, GroupBy is collect(groupingBy(...)), ToDictionary is collect(toMap(...))."
---

| C# | Java 25 |
| --- | --- |
| Where | filter |
| Select | map |
| GroupBy(...).ToDictionary(...) | collect(groupingBy(..., summingLong(...))) |
| Any | anyMatch |
| ToList() | toList() |
| OrderBy(x => x.Name) | sorted(Comparator.comparing(X::name)) |
