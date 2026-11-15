# Partition Rebalancer

A cluster splits its keys into `P` **fixed partitions**: `partitionFor(key)` is
`Math.floorMod(Hashing.hash64(key), P)` and never changes. What changes is which node **owns**
each partition. `PartitionMap` keeps that assignment balanced while moving as little as possible.

## `PartitionMap(int partitions)`

At least 1 partition (`IllegalArgumentException`). There are no nodes yet.

## `addNode(node)` → the moves

- The **first** node owns every partition and nothing moves: an empty list.
- With `N` nodes already, the new node must end with `P / (N + 1)` partitions (integer
  division). Take them one at a time: each time from the node that owns the **most** partitions
  (ties: the smallest name), and from that node its **highest-numbered** partition. Each move is
  a `Move(partition, from, to)`, in the order made.
- Adding a node that is already there throws `IllegalArgumentException`.

## `removeNode(node)` → the moves

- The node's partitions, **lowest-numbered first**, each go to the node that owns the **fewest**
  partitions at that moment (ties: the smallest name).
- An unknown node throws `IllegalArgumentException`; removing the last node throws
  `IllegalStateException` (its data would have nowhere to go).

## Queries

- `ownerOf(partition)`, `nodeFor(key)`: the owner (no nodes: `IllegalStateException`;
  a partition out of range: `IllegalArgumentException`).
- `assignment()`: every node (sorted by name) with its partitions (ascending); a node may own
  none when there are more nodes than partitions.

## What the rules guarantee

- Every partition has exactly one owner, and two nodes never differ by more than one partition.
- Adding a node moves partitions **only to** the new node; removing one moves them **only from**
  the removed node. Nothing else moves, so the data copied is the minimum.

Example with 6 partitions: `n1` owns 0–5; adding `n2` moves 5, 4 and 3 to it; adding `n3` moves
2 (from `n1`, which ties with `n2` and has the smaller name) and then 5 (from `n2`). Removing
`n2` (which owns 3 and 4) moves 3 to `n1` and 4 to `n3`.

> Tip: keep `String[] owner` indexed by partition and a `TreeMap<String, TreeSet<Integer>>` from
> node to partitions. The TreeMap's order gives you "smallest name" for ties; `last()` of a
> TreeSet is its highest partition.
