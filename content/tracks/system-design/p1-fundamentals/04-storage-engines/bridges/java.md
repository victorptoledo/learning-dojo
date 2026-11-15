---
from: java
status: draft
concept: "How data is stored and found"
from_way: "JPA entities and repositories hide the table, the index and the engine; a TreeMap is a sorted structure in memory."
native_way: "Pick the store from the access patterns, know whether its engine is a B-tree or an LSM tree, and design keys and indexes for the queries you will run."
sensei_tip: "A TreeMap is a balanced binary tree; a B-tree is the same idea with hundreds of keys per node so each level is one disk page. Guava's BloomFilter.create(funnel, expected, fpp) sizes a filter with the formulas of this lesson."
---

| Java | Storage concept |
| --- | --- |
| `TreeMap<K, V>` | a sorted index (in memory) |
| `@Index(columnList = "customer_id, created_at")` | a composite B-tree index |
| `@Lob byte[] photo` | a blob that belongs in object storage |
| Guava `BloomFilter` | the probabilistic filter LSM engines use |
| RocksDB JNI (`org.rocksdb`) | an embeddable LSM engine |
