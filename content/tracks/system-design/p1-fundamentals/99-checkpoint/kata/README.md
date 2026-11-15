# Sharded Cache Client

You write the client of a key-value cache spread over several **shards** (servers). The pieces
of this phase are given, already working: `HashRing` (with `nodesFor`), `BloomFilter` and
`Hashing`. You write `ShardedClient`.

```java
var client = new ShardedClient(shards, 100, 2, 3);   // 100 virtual nodes, 2 replicas, down after 3 failures
client.put("user:42", "Ada");                       // stored on 2 shards → returns 2
client.get("user:42");                              // Optional[Ada]
```

## `ShardedClient(Map<String, Shard> shards, int virtualNodes, int replicas, int failureThreshold)`

- Every shard name is added to a `HashRing(virtualNodes)`. All shards start **healthy**.
- No shards, or `virtualNodes`, `replicas` or `failureThreshold` below 1, throw
  `IllegalArgumentException`.
- The **preference list** of a key is `ring.nodesFor(key, replicas)`. The ring never changes:
  a shard that is down keeps its place and simply gets skipped, so its keys come back with it.

## `put(key, value)` → number of acknowledgements

- Writes to every **healthy** shard of the preference list, in order.
- A shard that throws `ShardUnavailableException` counts a **failure**; after
  `failureThreshold` **consecutive** failures it is marked **down**. A success resets its count.
- After a successful write, the key is added to that shard's **Bloom filter** (one
  `BloomFilter.forCapacity(100_000, 0.01)` per shard).
- If no shard acknowledged, throw `UnavailableException`.

## `get(key)` → `Optional<String>`

- Asks the **healthy** shards of the preference list, in order, and returns the first value found.
- **Skip** (do not call) a shard whose Bloom filter says the key was never written there, and
  count it in `skippedByFilter()`.
- A shard that throws counts a failure (as in `put`) and the next one is tried. If no shard has
  the value, return `Optional.empty()`: a read never throws.

## Health

- `healthy()`: the names of the healthy shards, sorted.
- `probe()`: calls `ping()` on every shard that is **down**; one that answers `true` is healthy
  again with its failure count reset. Healthy shards are not pinged.

The shards start empty and only this client writes to them, so the filters know every key a
shard can hold.

> Tip: keep a small mutable record per shard: its `Shard`, its filter, its consecutive failures
> and whether it is down. A `TreeMap<String, ShardState>` gives you `healthy()` already sorted.
