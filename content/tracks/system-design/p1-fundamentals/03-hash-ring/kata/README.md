# Hash Ring

A consistent hash ring places every node at `virtualNodes` points and every key at one point,
all on the same circle of 64-bit hash values. A key belongs to the **first node point at or
after the key's point**, wrapping around past the largest value to the smallest.

`Hashing.hash64(text)` is given: the first 8 bytes of the MD5 of the UTF-8 text, as a `long`.
Order points by the `long` value (the natural order of a `TreeMap<Long, String>`).

## `HashRing(int virtualNodes)`

- Rejects `virtualNodes < 1` with `IllegalArgumentException`.
- `add(node)`: places the node at the points `hash64(node + "#" + i)` for `i` from `0` to
  `virtualNodes - 1`. Adding a node twice changes nothing. A `null` or blank name throws
  `IllegalArgumentException`.
- `remove(node)`: removes its points; an unknown node is ignored.
- `nodes()`: the node names, sorted, as an unmodifiable set.
- `nodeFor(key)`: the owner of the key. An empty ring throws `IllegalStateException`.
- `nodesFor(key, count)`: the owner followed by the next **distinct** nodes clockwise, the
  replicas of the key. Never more than the number of nodes; `count` 0 gives an empty list, a
  negative `count` throws `IllegalArgumentException`.

## The properties the tests check

- The same key always maps to the same node.
- With 4 nodes and 200 virtual nodes each, every node owns between 18 % and 32 % of 20,000 keys.
- Adding a fourth node moves between 15 % and 35 % of the keys, and **every** moved key moves
  to the new node.
- Removing a node moves **only** the keys it owned.

> Tip: `TreeMap.ceilingEntry(h)` finds the first point at or after `h`; when it returns `null`
> you have passed the end of the circle, so take `firstEntry()`. For `nodesFor`, walk
> `tailMap(h)` and then `headMap(h)`, collecting names into a `LinkedHashSet` until it has
> `count` of them.
