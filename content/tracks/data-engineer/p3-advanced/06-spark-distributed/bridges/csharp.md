---
from: csharp
status: draft
concept: "Distributed in-memory computing"
from_way: "LINQ over in-memory collections or Entity Framework queries."
native_way: "Spark DataFrames (in Synapse, Fabric or Databricks notebooks), lazy and distributed."
sensei_tip: "LINQ's deferred execution is Spark's lazy evaluation on one machine: nothing runs until you enumerate, and in Spark the 'enumeration' is an action that may move terabytes."
---

| C# | Apache Spark |
| --- | --- |
| IQueryable deferred execution | lazy evaluation |
| ToList() | an action (collect: careful) |
| GroupBy in LINQ | a shuffle |
| Join with a small Dictionary | a broadcast join |
| Parallel.ForEach | tasks over partitions |
