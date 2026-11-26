# Bit Manipulation

Every integer is a row of bits, and the CPU can operate on all 64 of them in one instruction.
Bit manipulation uses that to store sets of flags in a single word, to test and combine them in
O(1), and to answer some interview puzzles with one line. It also explains a few Java surprises
that bite even senior developers.

## How Java stores integers

`int` is 32 bits and `long` 64, both **signed** in **two's complement**: the top bit counts as
−2³¹ (or −2⁶³). So:

```text
 5 = 0000 0101
-5 = 1111 1011      (invert all bits of 5 and add 1:  -x == ~x + 1)
-1 = 1111 1111      (all ones)
```

Consequences: `Integer.MAX_VALUE + 1 == Integer.MIN_VALUE` (silent overflow), `Math.abs(MIN_VALUE)`
is still negative, and `byte` values above 127 are negative (mask with `b & 0xFF` to read them as
0–255). `Math.addExact` and friends throw instead of overflowing when you need that.

## The operators

| Operator | Meaning | Example (8 bits) |
| --- | --- | --- |
| `a & b` | AND: 1 where both are 1 | `1100 & 1010 = 1000` |
| `a \| b` | OR: 1 where either is 1 | `1100 \| 1010 = 1110` |
| `a ^ b` | XOR: 1 where they differ | `1100 ^ 1010 = 0110` |
| `~a` | NOT: flip every bit | `~0000 1111 = 1111 0000` |
| `a << k` | shift left: multiply by 2ᵏ | `0000 0011 << 2 = 0000 1100` |
| `a >> k` | arithmetic shift right: divide by 2ᵏ rounding down, sign kept | `-8 >> 1 = -4` |
| `a >>> k` | logical shift right: zeros shifted in | `-8 >>> 1 = 2147483644` |

Two Java traps: the shift distance is taken **mod 32** for `int` and **mod 64** for `long`, so
`1 << 32 == 1`; and `1 << 40` is computed on an `int` before any widening. Write `1L << 40`.

## The classic tricks

| Goal | Expression |
| --- | --- |
| test bit i | `(x >> i & 1) == 1`, or `(x & (1 << i)) != 0` |
| set bit i | `x \| (1 << i)` |
| clear bit i | `x & ~(1 << i)` |
| toggle bit i | `x ^ (1 << i)` |
| lowest set bit | `x & -x` |
| clear the lowest set bit | `x & (x - 1)` |
| is x a power of two (x > 0) | `(x & (x - 1)) == 0` |
| count set bits | `Integer.bitCount(x)` (one CPU instruction, POPCNT) |
| index of the lowest / highest set bit | `Integer.numberOfTrailingZeros(x)`, `31 - Integer.numberOfLeadingZeros(x)` |
| next power of two ≥ x | `Integer.highestOneBit(x - 1) << 1` (for x > 1) |
| even or odd | `(x & 1) == 0` |
| average without overflow | `(a + b) >>> 1` for non-negative ints |

### XOR, the cancelling operator

XOR is its own inverse: `x ^ x = 0`, `x ^ 0 = x`, and the order does not matter. So:

- XOR of all values where every value appears twice except one leaves the single one.
- The missing number in `0..n` is the XOR of `0..n` XOR the XOR of the array.
- Swap without a temporary: `a ^= b; b ^= a; a ^= b;` (a party trick, not better code).
- Two values that differ: `a ^ b` has a 1 exactly where they differ; `Integer.bitCount(a ^ b)` is
  their **Hamming distance**.

## Bitmasks as small sets

A set of up to 64 elements fits in a `long`: bit i means "element i is in". Union is `|`,
intersection `&`, difference `& ~`, size `bitCount`, membership one AND. Enumerate:

```java
for (int mask = 0; mask < (1 << n); mask++) { ... }          // every subset of n elements
for (int sub = mask; sub > 0; sub = (sub - 1) & mask) { ... } // every non-empty subset of mask
for (int m = mask; m != 0; m &= m - 1) {                       // each set bit of mask
    int i = Integer.numberOfTrailingZeros(m);
}
```

**Bitmask DP** uses masks as states: the travelling salesman over n ≤ 20 cities is
`dp[visitedMask][lastCity]`, O(2ⁿ · n²), far better than n!. Assigning n tasks to n workers,
covering a set with the fewest pieces and many puzzle searches use the same idea.

## Bits in real systems

- **Flags and permissions**: Unix file modes (`chmod 755` is three 3-bit masks), network
  protocol headers (TCP flags), `java.lang.reflect.Modifier`, `EnumSet` (a single `long` for
  enums of up to 64 constants).
- **`BitSet`** stores a boolean array 64 to a `long`: a sieve of Eratosthenes over 100 million
  numbers fits in about 12 MB.
- **Bloom filters** set k bits per key in a bit array; databases (Cassandra, RocksDB) skip disk
  reads with them.
- **Bitmap indexes and Roaring bitmaps** answer "users with feature A and B" as an AND of two
  bitmaps; Elasticsearch, Druid, Pinot and Spark use them.
- **Hashing**: `HashMap` spreads hashes with `h ^ (h >>> 16)` and indexes with `h & (n - 1)`.
- **Fenwick trees** (binary indexed trees) walk with `i & -i` to answer prefix sums with updates
  in O(log n).
- **Graphics and compression** pack pixels and codes into words; Huffman codes are bit strings.

## Summary

- Java integers are signed two's complement; overflow is silent.
- `&`, `|`, `^`, `~`, `<<`, `>>` (sign-keeping) and `>>>` (zero-filling); shift distances wrap.
- Learn the tricks: lowest bit `x & -x`, clear it `x & (x - 1)`, power of two, `bitCount`.
- XOR cancels pairs: find the single, the missing, the differing.
- A `long` is a set of 64; masks enumerate subsets and power bitmask DP; `BitSet` and bitmaps
  scale it up.

## Further reading

- cp-algorithms, "Bit manipulation": <https://cp-algorithms.com/algebra/bit-manipulation.html>
- cp-algorithms, "Fenwick Tree": <https://cp-algorithms.com/data_structures/fenwick.html>
- JDK 25 API, `Integer` (bitCount, numberOfTrailingZeros, highestOneBit, rotateLeft): <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Integer.html>
- JDK 25 API, `BitSet`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/BitSet.html>
- The Java Language Specification, "Shift Operators" (distance masking): <https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.19>
- Sean Eron Anderson, "Bit Twiddling Hacks": <https://graphics.stanford.edu/~seander/bithacks.html>
