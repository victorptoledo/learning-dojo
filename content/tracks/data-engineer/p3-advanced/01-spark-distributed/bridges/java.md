---
from: java
status: draft
concept: "Distributed in-memory computing"
from_way: "Loading a 50 GB file into one process and running out of memory."
native_way: "Spark splits data into partitions and spreads the computation across many executor nodes."
sensei_tip: "Spark's worst enemy is data skew, when one partition gets most of the data. Salting keys rebalances it."
---
