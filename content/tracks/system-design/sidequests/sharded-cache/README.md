# Sidequest: Sharded cache with live resharding

A cache in front of a database works until one cache server is not enough. Then the keys are
spread over many cache nodes, and the interesting problems start: adding a node to a warm
cluster must not turn into a flood of misses on the database, one viral key must not melt the
single node that owns it, and a dying node or a slow database must not take the whole site
down with them.

Build `dojo.cluster.CacheCluster`, the client every application server uses to talk to the
cache nodes. It is a **read-through** cache: `get(key)` answers from the node that owns the
key, and on a miss it loads the value from the database and stores it on that node.

The starter gives you the contract types:

- `CacheNode`, one cache server (`get`, `put`, `remove`, and `keys`, a full scan that is
  expensive on a real node). The tests pass nodes in memory that count every call, and can
  make them fail.
- `Origin`, the database: `load(key)` is slow and is what the cache protects.
- `Hashing.hash64`, the same stable hash as the hash ring kata.
- `ClusterConfig`, the numbers of the design. The tests build it with the settings each stage
  needs.

Time comes from the `java.time.Clock` given to the constructor; the tests move it by hand.

Use the JDK only.
