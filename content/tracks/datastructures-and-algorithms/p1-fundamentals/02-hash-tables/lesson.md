# Hash Tables Inside: Hash Functions, Collisions and Load Factor

A hash table answers "what is stored under this key?" in **O(1) expected time**, whatever the
number of keys. It is the most used data structure in backend code: `HashMap`, `HashSet`, caches,
deduplication, joins in a database, symbol tables in a compiler. This lesson opens it up so you
know when the O(1) holds and when it quietly does not.

## The idea

Keep an array of `m` **buckets**. A **hash function** turns any key into an integer; reduce it to
an index in `0..m-1` and keep the entry there.

```text
"ada"  → hashCode 96414   → bucket 96414 & 15 = 14
"bob"  → hashCode 97717   → bucket 97717 & 15 = 5
"carl" → hashCode 3046168 → bucket 3046168 & 15 = 8
```

Two different keys can land in the same bucket: a **collision**. Collisions are unavoidable
(there are more possible keys than buckets), so every hash table needs a strategy for them.

## Hash functions and the contract

A good hash function is **deterministic**, **fast** and **spreads** keys evenly over the buckets.
In Java the key provides it through `hashCode()`, together with `equals()`:

- **Equal objects must have equal hash codes.** If they do not, the map looks in the wrong
  bucket and misses.
- Different objects *may* share a hash code; that is just a collision.
- Use the same fields in both. `record`s and `Objects.hash(a, b, c)` get this right for free.
- **Keys must not change** while they are in a map. A key whose hash changes stays in the bucket
  of its old hash and becomes unreachable.

`java.util.HashMap` does not trust `hashCode()` to have good low bits. It mixes the high half
into the low half, `h ^ (h >>> 16)`, then takes `h & (m - 1)` with a power-of-two `m`. Without
that step, keys such as `i * 65536` (all low bits zero) would all share bucket 0. You implement
exactly this in the next kata.

## Two ways to handle collisions

### Separate chaining

Each bucket holds a small list of the entries that hash there. Insert at the head, search the
list with `equals`. Simple, tolerant of high load, and deleting is easy. `java.util.HashMap`,
`ConcurrentHashMap` and most Java maps chain.

### Open addressing

Everything lives in the array itself. On a collision, **probe** other slots in a fixed order
until a free one appears:

| Probing | Next slot | Note |
| --- | --- | --- |
| Linear | `i, i+1, i+2, ...` | cache friendly, but clusters grow |
| Quadratic | `i, i+1, i+4, i+9, ...` | breaks up clusters |
| Double hashing | `i, i+h₂, i+2h₂, ...` | second hash decides the step |
| Robin Hood | linear, but a "poorer" key may evict a "richer" one | evens out probe lengths |

Deletion needs **tombstones** (a marker that says "keep probing") or shifting entries back.
Open addressing uses less memory and fewer pointers; Python's `dict`, Rust's `HashMap`
(SwissTable), Go's maps since Go 1.24 and many high-performance libraries use it.

## Load factor and rehashing

The **load factor** is `entries / buckets`. As it rises, chains get longer (or probe sequences
get longer), and lookups slow down. So the table **resizes**: when the load factor passes a
threshold (0.75 in `HashMap`), it allocates twice the buckets and moves every entry to its new
bucket.

- One resize is O(n), but doubling makes it **amortised O(1)** per insert: the dynamic array
  argument again.
- With chaining, the expected chain length is the load factor, so lookups are O(1 + α).
- Open addressing must stay well below 1 (often 0.5 to 0.875): at a load factor near 1, probes
  explode.
- If you know the size, pre-size the table: `HashMap.newHashMap(expectedEntries)` (Java 19+)
  computes the capacity for you.

## When O(1) is not O(1)

- **Bad hash codes**: a `hashCode()` that returns few distinct values (a length, a constant, a
  boolean) piles keys into a few buckets, and lookups become O(n).
- **Hash flooding**: an attacker who knows your hash function can send thousands of keys that
  collide (for example HTTP parameter names) and turn each request into O(n²) work. Defences are
  randomised hashing (Python and Rust seed their string hash per process) and, in Java 8+,
  **treeification**: a bucket with more than 8 entries (in a table of at least 64 buckets)
  becomes a red-black tree, so the worst case per bucket is O(log n). That works best when the
  keys are `Comparable`.
- **Resize pauses**: one insert that triggers a resize of a huge table can take milliseconds.
  Latency-sensitive systems pre-size or resize incrementally (Redis rehashes its dictionaries a
  few buckets at a time).
- **Iteration order** of a `HashMap` is the bucket order: it looks random and can change after a
  resize. Use `LinkedHashMap` to keep insertion order and `TreeMap` for sorted order.

## Choosing the right map in Java

| You need | Use | Cost |
| --- | --- | --- |
| fast lookup, any order | `HashMap` / `HashSet` | O(1) expected |
| insertion order, or an LRU cache | `LinkedHashMap` (with `accessOrder` and `removeEldestEntry`) | O(1) expected |
| sorted keys, floor/ceiling, ranges | `TreeMap` / `TreeSet` | O(log n) |
| many threads | `ConcurrentHashMap` | O(1) expected, lock-free reads |
| enum keys | `EnumMap` / `EnumSet` | an array indexed by ordinal |
| identity, not equals | `IdentityHashMap` | open addressing on `System.identityHashCode` |

## In real systems

- Databases use hash joins (build a hash table on the smaller table, probe it with the larger)
  and hash indexes (PostgreSQL `USING hash`).
- Load balancers and caches hash keys to servers (consistent hashing, in the System Design
  track).
- Deduplication pipelines keep a hash set of seen ids, or a Bloom filter when memory is tight.

## Summary

- A hash table maps a key to a bucket with a hash function and resolves collisions by chaining
  or open addressing.
- Equal keys must have equal hash codes, and keys must not change while stored.
- Resizing at a load factor keeps operations O(1) expected and amortised.
- Bad hashes and adversarial keys break the O(1); Java treeifies long buckets as a safety net.
- Pick `HashMap`, `LinkedHashMap`, `TreeMap` or `ConcurrentHashMap` by the order and concurrency
  you need.

## Further reading

- Sedgewick and Wayne, *Algorithms*, 4th ed., "Hash Tables": <https://algs4.cs.princeton.edu/34hash/>
- Morin, *Open Data Structures*, chapter "Hash Tables": <https://opendatastructures.org/>
- JDK 25 API, `HashMap` (load factor, capacity, iteration order): <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/HashMap.html>
- JDK 25 API, `Object.hashCode` (the contract): <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Object.html#hashCode()>
- MIT OCW 6.006 Introduction to Algorithms (Spring 2020), lectures on hashing: <https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/>
- roadmap.sh, DSA topic "Hash Tables": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/hash-tables@kSy-8ubESdmIX4Fyl8TSu.md>
