---
from: java
status: draft
concept: "Distributed in-memory computing"
from_way: "Loading a 50 GB file into one JVM with streams and running out of heap."
native_way: "Spark splits data into partitions and spreads the computation across many executor JVMs, planning it lazily."
sensei_tip: "Spark's worst enemy is data skew, when one partition gets most of the data. The Java Stream API is lazy too, but it runs in one JVM; Spark's Dataset API in Java and Scala distributes the same style of pipeline."
---

| Java | Apache Spark |
| --- | --- |
| list.stream().filter().collect() | a lazy pipeline, on one machine |
| Dataset<Row>.filter(...) | a lazy, distributed transformation |
| Collectors.groupingBy | groupBy: a shuffle in Spark |
| a HashMap lookup in a loop | a broadcast hash join |
| ForkJoinPool parallelism | tasks per partition on executors |
