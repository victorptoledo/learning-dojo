Start with the routing and the read-through.

```java
public CacheCluster(Origin origin, Clock clock, ClusterConfig config)
public void addNode(String name, CacheNode node, int weight)
public void removeNode(String name)
public String ownerOf(String key)
public String get(String key)
public void invalidate(String key)
```

**The ring.** Every application server must route a key to the same node, so the ring is
fixed by these rules (the tests compute it on their own):

- A node of weight `w` has `w × config.virtualNodes()` points; point `i` (from 0) sits at
  `Hashing.hash64(name + "#" + i)`.
- A key sits at `Hashing.hash64(key)`. Its owner is the node of the first point at or after
  it, comparing positions as signed `long`s, wrapping around to the smallest point. (Two
  points at the same position: the smaller node name wins.)
- A node with twice the weight gets about twice the keys. Adding or removing a node only
  moves the keys that node gains or loses.

**Read-through.**

- `get` asks the owner. On a hit, that is the answer. On a miss it calls `origin.load(key)`,
  puts the value on the owner and returns it: the next `get` of the key is a hit.
- `invalidate` removes the key from the cache (the database changed it): the next `get`
  loads it again.
- `removeNode` takes the node off the ring at once; its keys are simply missing (cold) on
  their new owners. Stage 2 makes adding a node smarter than that.

**Errors.** `get` and `ownerOf` on a cluster without nodes throw `IllegalStateException`. A
weight below 1, a blank name or a name already in the cluster throws
`IllegalArgumentException`, and so does removing a node that is not there.
