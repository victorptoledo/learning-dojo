# Text Search

Five functions over strings, from the KMP prefix function to rolling hashes. Positions are
`char` indexes of the Java `String`.

```java
TextSearch.prefixFunction("ababaca");              // [0, 0, 1, 2, 3, 0, 1]
TextSearch.findAll("aaaa", "aa");                  // [0, 1, 2]   overlapping matches count
TextSearch.smallestPeriod("abcabcabc");            // 3
TextSearch.smallestPeriod("abcab");                // 5   no shorter block repeats exactly
TextSearch.countAll("banana", List.of("ana", "nan", "xyz"));  // {ana=2, nan=1, xyz=0}
TextSearch.longestRepeated("banana");              // "ana"  (at 1 and 3: occurrences may overlap)
```

## What to write

| Method | Returns |
| --- | --- |
| `prefixFunction(p)` | `pi[i]` = the length of the longest proper prefix of `p[0..i]` that is also a suffix of it; an empty array for `""` |
| `findAll(text, pattern)` | every start of `pattern` in `text`, **overlapping** matches included, in increasing order |
| `smallestPeriod(s)` | the smallest `p` such that `s` is its first `p` characters repeated a whole number of times (`s.length()` if none is shorter); 0 for `""` |
| `countAll(text, patterns)` | a map from **every** distinct pattern to its number of (overlapping) occurrences, 0 included. All patterns must have the same length. |
| `longestRepeated(s)` | the longest substring that occurs at least twice (occurrences may overlap); among the longest, the one whose **first occurrence starts earliest**; `""` when no character repeats |

`null` arguments, an empty pattern (in `findAll` and `countAll`) and patterns of different lengths
in `countAll` throw `IllegalArgumentException`.

## How it is graded

The hidden tests compare every function with brute force on 600 small random strings over
alphabets of one to three letters (so there are many repeats and borders), then time:

- `findAll` of 20,000 `a`s in 400,000 `a`s (380,001 matches) and of `a…ab` ten times: a restart at
  every position is O(n · m);
- `countAll` with 4,000 patterns of 5,000 characters, and with 40 patterns of 60,000 characters,
  in a million characters: one search per pattern, or hashing or copying each window from
  scratch, is too slow;
- `longestRepeated` on 300,000 characters and on a 150,000-character string written twice.

> Tip: for `countAll` and `longestRepeated`, roll one polynomial hash modulo a large prime
> (`2^61 - 1` with `Math.multiplyHigh`, or two 31-bit primes) and **compare the characters**
> whenever two hashes are equal: a collision then costs time, never a wrong answer.
