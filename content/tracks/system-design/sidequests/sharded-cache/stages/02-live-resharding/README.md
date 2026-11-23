The cluster is warm: thousands of keys are cached and the database only sees a trickle of
misses. Add a fifth node with `addNode` and a fifth of the keys are suddenly missing: a
stampede on the database at the worst moment (you add nodes when traffic is high). The values
are still there, on the nodes that used to own them. Move them instead of losing them.

`addNode` stays as it is, for building a cluster before it gets traffic. A node joins a live
cluster with:

```java
public void joinNode(String name, CacheNode node, int weight)   // same checks as addNode
public int migrateStep(int maxKeys)   // moves up to maxKeys keys; returns how many it moved
public boolean migrating()
```

**Joining starts a migration**

- Ownership moves at once: `ownerOf` answers with the new ring, and new values are stored on
  the new owner.
- While migrating, a `get` that misses on a key's new owner asks the key's **previous owner**
  (its owner on the ring before the join). A hit there is copied to the new owner, removed
  from the previous one, and returned: no database load.
- `invalidate` removes the key from its owner and, during a migration, from its previous
  owner too. Otherwise the old value would come back from the previous owner on the next
  read.

**Background copy**

- `migrateStep` moves keys that now belong to the new node from the nodes that hold them:
  copy to the new node, then remove from the old one. It moves at most `maxKeys` keys per
  call, so the copy never competes too hard with real traffic.
- A full scan (`CacheNode.keys()`) is expensive: each old node is scanned once per migration,
  not once per step.
- Keys a `get` already moved are not moved again and do not count. Keys on an old node that
  still belong to it stay where they are.
- `migrating()` is `true` from the join until a step runs out of keys to move. After that,
  reads of the moved keys only go to the new node. `migrateStep` without a migration
  returns 0.
- `addNode`, `joinNode` or `removeNode` while a migration is running throws
  `IllegalStateException`: finish the migration first. Joining an empty cluster is just an
  add: there is nothing to migrate.
