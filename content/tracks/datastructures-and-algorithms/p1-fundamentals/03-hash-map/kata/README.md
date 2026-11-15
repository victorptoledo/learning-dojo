# Hash Map

Write `ChainedHashMap<K, V>`, a hash map with **separate chaining**: an array of buckets, each
the head of a singly linked list of entries. Do not use `java.util.HashMap` or any other map
inside it. The `Entry` class you need is up to you.

```java
var ages = new ChainedHashMap<String, Integer>();
ages.put("ada", 36);          // null (no previous value)
ages.put("ada", 37);          // 36
ages.get("ada");              // 37
ages.remove("ada");           // 37
ages.get("ada");              // null
```

## The rules

- A new map has **16 buckets** (`bucketCount()`), and the bucket count is always a power of
  two.
- The bucket of a key is `spread(key.hashCode()) & (bucketCount - 1)` with
  `spread(h) = h ^ (h >>> 16)`, the same mixing step as `java.util.HashMap`.
- Inside a bucket, keys are compared with `equals`, never `==`.
- After a `put` that **adds** a key, if `size > bucketCount * 0.75`, the bucket count doubles
  and every entry is moved to its bucket in the new array.
- `null` keys throw `IllegalArgumentException`; `null` values are allowed.

## Methods

| Method | Returns |
| --- | --- |
| `put(key, value)` | the previous value, or `null` |
| `get(key)` | the value, or `null` when the key is missing |
| `containsKey(key)` | whether the key is present (also when its value is `null`) |
| `remove(key)` | the removed value, or `null` |
| `size()`, `isEmpty()` | |
| `bucketCount()` | the length of the bucket array |
| `longestChain()` | the number of entries in the longest bucket (for the tests) |
| `keys()` | all keys, in any order |

## What the hidden tests check

- Random operations against `java.util.HashMap`.
- Keys whose `hashCode` is always the same still work (slowly), because `equals` decides.
- After 100,000 keys the bucket count is 262,144 (`size <= 0.75 × buckets` after every put).
- 65,536 `Integer` keys that are multiples of 65,536 keep `longestChain()` small: they only
  differ in their high bits, so without `spread` they would all land in bucket 0.
- A million puts and a million gets finish within a time limit.

> Tip: insert new entries at the **head** of the chain: it is O(1) and the order inside a
> bucket does not matter.
