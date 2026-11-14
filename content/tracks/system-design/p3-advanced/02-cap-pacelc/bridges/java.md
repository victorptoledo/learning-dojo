---
from: java
status: draft
concept: "Theoretical trade-offs of distributed systems"
from_way: "Assuming perfect local ACID transactions for everything."
native_way: "In distributed systems partitions (P) will happen; during one you must choose consistency (C) or availability (A)."
sensei_tip: "PACELC is more realistic than CAP: even without a partition (Else) there is a constant trade-off between latency (L) and consistency (C)."
---
