# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): the client of a sharded key-value cache. It
   places keys on a consistent hash ring with replicas, marks shards down after repeated
   failures and probes them back, and keeps a Bloom filter per shard to skip reads that cannot
   succeed.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| DNS, Anycast and Edge CDNs | What does a TTL trade? When is DNS too coarse? Which responses can a CDN share? |
| Load Balancers and Consistent Hashing | L4 or L7? What should a health check test? Why does `hash mod N` hurt? |
| Hash Ring (kata) | Which keys move when a node joins or leaves? How are replicas chosen? |
| Storage Engines | B-tree or LSM for this workload? Local or global index? |
| Bloom Filter (kata) | What does "definitely not" buy you? How many bits per key for 1 %? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
