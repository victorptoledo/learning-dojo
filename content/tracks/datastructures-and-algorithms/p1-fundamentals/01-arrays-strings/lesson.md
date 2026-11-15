# Arrays, Strings and Their Costs

The array is the structure every other structure is built from: hash tables are arrays of
buckets, heaps are arrays read as trees, dynamic arrays are arrays that get replaced. Strings
are arrays of characters with a few rules of their own. Knowing exactly what each operation
costs on them is the first step in reading any algorithm.

## What an array is

An array is a block of **contiguous** memory holding `n` elements of the same size. The address
of element `i` is `base + i × size`, so reading or writing any index is **O(1)**, and scanning
from left to right reads memory in order, which CPU caches and prefetchers make very fast.

| Operation | Cost | Why |
| --- | --- | --- |
| `a[i]`, `a[i] = x` | O(1) | address arithmetic |
| scan all elements | O(n) | and cache friendly |
| find a value (unsorted) | O(n) | must look at each |
| find a value (sorted) | O(log n) | binary search, phase 2 |
| insert or delete at index i | O(n − i) | shift everything after i |
| append (dynamic array) | O(1) amortised | the previous kata |

In Java, an `int[]` stores the numbers themselves, while an `Integer[]` or `ArrayList<Integer>`
stores **references** to boxed objects spread over the heap: more memory, an extra indirection
and worse cache behaviour. For large numeric data, primitive arrays are often several times
faster.

## Two dimensions

`int[][] grid = new int[rows][cols]` in Java is an array of row arrays. Walking it **row by row**
(`grid[r][c]` with `c` in the inner loop) follows memory; walking it column by column jumps
between rows. Many problems flatten a grid into one array with `index = r * cols + c`, which you
will do in the BFS kata of phase 3.

## Prefix sums: precompute once, answer in O(1)

When many queries ask for the sum of a range, precompute `prefix[k] = a[0] + ... + a[k-1]`
(length `n + 1`, with `prefix[0] = 0`). Then:

```text
sum(a[i..j]) = prefix[j + 1] - prefix[i]        O(1) per query after O(n) setup
```

The same idea works in 2D (summed-area tables, used in image processing) and with other
reversible operations such as XOR. Combined with a hash map it counts subarrays with a given
sum, which the Sequence Toolkit kata asks for. Use `long` for the sums: a year of hourly counts
overflows an `int` sooner than you think.

## In-place techniques

Many array problems ask you to work **in place**, with O(1) extra memory:

- **Reverse**: swap `a[i]` and `a[n-1-i]` for `i < n/2`.
- **Rotate by k**: reverse the whole array, then reverse the first k and the rest.
- **Remove elements**: a "write" index that only advances when you keep an element; everything
  after it is garbage. This is the first taste of the **two pointers** technique.
- **Counting instead of sorting**: when values come from a small range (letters, digits, ages),
  an `int[]` of counts replaces a sort with one O(n) pass.

## Strings

A Java `String` is an **immutable** sequence of UTF-16 code units (`char`). Since Java 9, a
string whose characters all fit in Latin-1 is stored compactly with one byte per character
("compact strings"), but the API is the same.

- **Immutability** makes strings safe to share and to use as map keys, and lets the JVM cache
  their hash code. It also means every "modification" builds a new string: `s + x` copies `s`.
  A loop of `+=` is O(n²) characters copied; `StringBuilder` (a dynamic array of chars) makes it
  O(n).
- **Comparison**: `equals` compares contents in O(n); `==` compares references and is almost
  always a bug. `compareTo` is lexicographic by UTF-16 code unit, which is not how a dictionary
  in any human language sorts; use a `Collator` for that.
- **Substrings** copy the characters (since Java 7u6), so `substring` in a loop costs its length
  each time.
- **Characters are not code points**: an emoji or a rare CJK character outside the Basic
  Multilingual Plane is a **surrogate pair** of two `char`s. `length()` counts code units;
  `codePointCount` and `codePoints()` count what users see as characters (almost: accents can be
  separate combining marks, which `java.text.Normalizer` deals with).
- **Case**: `toLowerCase()` uses the default locale, and in Turkish "I" does not lower to "i".
  Use `toLowerCase(Locale.ROOT)` for identifiers and protocol text.

### String hashing in one paragraph

`String.hashCode()` is `s[0]·31ⁿ⁻¹ + s[1]·31ⁿ⁻² + ... + s[n-1]`, computed once and cached. That
polynomial idea is also the basis of **rolling hashes** (Rabin–Karp): the hash of the next
window of a string is computed from the previous one in O(1), which finds a pattern in a text in
O(n + m) expected time.

## Patterns that show up in interviews

| Problem shape | Tool |
| --- | --- |
| "how many of each", anagrams, first unique character | count array or `HashMap` |
| "sum or average of a range", many queries | prefix sums |
| "reverse", "rotate", "remove in place" | swaps and a write index |
| "is it a palindrome" | two indexes moving towards each other |
| "build a long string" | `StringBuilder`, never `+=` in a loop |
| "longest substring with some property" | sliding window, two lessons ahead |

## In the JDK and in real systems

- `Arrays.fill`, `Arrays.copyOf`, `System.arraycopy` (an intrinsic that copies memory in bulk),
  `Arrays.equals`, `Arrays.hashCode` and `Arrays.mismatch` are the array toolbox.
- `String.join`, `String.repeat`, `StringBuilder` and `text blocks` cover building text.
- Columnar databases and analytics engines (Parquet, Arrow, ClickHouse) store each column as a
  primitive array precisely to get cache-friendly scans.
- Java's `ByteBuffer` and Netty's buffers are arrays with read and write indexes, used for all
  network I/O.

## Summary

- Arrays give O(1) indexing and fast scans, but O(n) inserts and deletes in the middle.
- Primitive arrays beat boxed collections for large numeric data.
- Prefix sums turn range-sum queries into O(1) after O(n) setup; use `long`.
- Strings are immutable: build them with `StringBuilder`, compare with `equals`.
- `length()` counts UTF-16 units, not user-visible characters; mind locales when changing case.

## Further reading

- Sedgewick and Wayne, *Algorithms*, 4th ed., "Strings": <https://algs4.cs.princeton.edu/50strings/>
- cp-algorithms, "String Hashing": <https://cp-algorithms.com/string/string-hashing.html>
- JDK 25 API, `String`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html>
- JDK 25 API, `Arrays`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Arrays.html>
- roadmap.sh, DSA topic "Array": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/array@lxY3ErxJ_D3zkSAXIBUpv.md>
