# Bloom Filter

A Bloom filter is an array of `m` bits and `k` hash functions. `add` sets `k` bits; `mightContain`
answers `false` if any of them is 0 (the item was **never** added) and `true` otherwise (it was
**probably** added).

## The bit positions

`Hashing.hash128(text)` is given: two `long`s from the MD5 of the text. Derive the `k` positions
with **double hashing** (Kirsch and Mitzenmacher): for `i` from `0` to `k - 1`,

```text
position(i) = Math.floorMod(h1 + i * h2, m)
```

## `BloomFilter(int bits, int hashes)`

- `bits` and `hashes` must be at least 1 (`IllegalArgumentException`).
- `add(item)`, `mightContain(item)`.
- `bitCount()` is `m`, `hashCount()` is `k`, `bitsSet()` is how many bits are 1 now.
- `itemsAdded()` counts the calls to `add` (the filter cannot tell repeats apart).
- `expectedFalsePositiveRate()`: `(1 − e^(−k·n/m))^k` with `n = itemsAdded()`; `0` when empty.

## `BloomFilter.forCapacity(expectedItems, falsePositiveRate)`

Sizes the filter for `n` items at rate `p`:

```text
m = ceil(−n · ln p / (ln 2)²)        k = max(1, round(m / n · ln 2))
```

`forCapacity(1000, 0.01)` has 9,586 bits and 7 hashes. `n` below 1, `p` not strictly between 0
and 1, or an `m` that does not fit an `int` throw `IllegalArgumentException`.

The tests add 10,000 items to `forCapacity(10_000, 0.01)` and check that **none** of them is
reported missing and that fewer than 2 % of 100,000 other strings are reported present.

> Tip: `java.util.BitSet` gives you `set`, `get` and `cardinality`. Keep `h1 + i * h2` in `long`
> arithmetic and only then `floorMod` by `m`: overflow is fine, it is still a good spread.
