# String Matching: KMP, Rolling Hashes and Friends

"Find every place where this pattern appears in this text" sounds like one call to
`indexOf`. It is, until the text is a 2 GB log, the pattern is long and repetitive, or there
are ten thousand patterns. This lesson covers the algorithms behind `grep`, intrusion detection,
plagiarism checkers and `rsync`, and the costs that decide between them. Throughout, `n` is the
length of the text and `m` the length of the pattern.

## The naive search and its worst case

Try every start, compare character by character, stop at the first mismatch:

```java
for (int start = 0; start + m <= n; start++) {
    int i = 0;
    while (i < m && text.charAt(start + i) == pattern.charAt(i)) i++;
    if (i == m) found(start);
}
```

On ordinary text the first characters rarely match, so it runs in about O(n). The worst case is
O(n · m): text `aaaa…a` and pattern `aaa…ab` match `m - 1` characters at every start before
failing. Java's `String.indexOf` is this algorithm with fast vectorised comparisons, so it has
the same worst case, and calling it in a loop to count overlapping matches pays it again for
every match.

## Knuth-Morris-Pratt: never look back

When a match fails after `i` matched characters, the naive search throws that knowledge away.
KMP keeps it. The **prefix function** `pi[i]` of the pattern is the length of the longest proper
prefix of `pattern[0..i]` that is also a suffix of it (a **border**):

```text
pattern  a b a b a c a
pi       0 0 1 2 3 0 1
```

After a mismatch with `j` characters matched, the next candidate is `pi[j - 1]` matched
characters, not zero, and the text pointer never moves back:

```java
static int[] prefixFunction(String p) {
    int[] pi = new int[p.length()];
    for (int i = 1, k = 0; i < p.length(); i++) {
        while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
        if (p.charAt(i) == p.charAt(k)) k++;
        pi[i] = k;
    }
    return pi;
}

static List<Integer> findAll(String text, String p) {   // overlapping matches
    int[] pi = prefixFunction(p);
    List<Integer> starts = new ArrayList<>();
    for (int i = 0, k = 0; i < text.length(); i++) {
        while (k > 0 && text.charAt(i) != p.charAt(k)) k = pi[k - 1];
        if (text.charAt(i) == p.charAt(k)) k++;
        if (k == p.length()) {
            starts.add(i - k + 1);
            k = pi[k - 1];                             // keep going: matches may overlap
        }
    }
    return starts;
}
```

Each step either advances `i` or shrinks `k`, and `k` grows by at most one per step, so the total
is **O(n + m)** in every case. The prefix function answers more than search:

- **Smallest period** of a string `s`: `p = n - pi[n - 1]`. If `n % p == 0`, `s` is a repetition of
  its first `p` characters (`abcabcabc` has period 3); otherwise no shorter block repeats exactly.
- **Streaming**: KMP reads the text one character at a time and keeps only `k`, so it can scan a
  log line by line, or a network stream byte by byte, without holding the text.
- The **Z-function** is a cousin: `z[i]` is the length of the longest common prefix of `s` and
  `s.substring(i)`. Search for `p` in `t` by computing it on `p + '\0' + t`.

## Rabin-Karp: compare numbers, not strings

A **rolling hash** turns each window of `m` characters into a number that can be updated in O(1)
when the window slides:

```text
h(s[i..i+m)) = s[i]·B^(m-1) + s[i+1]·B^(m-2) + … + s[i+m-1]   (mod M)
slide:  h' = (h - s[i]·B^(m-1)) · B + s[i+m]                   (mod M)
```

Compare the window's hash with the pattern's; only when they are equal compare the characters,
because different strings can share a hash (a **collision**). With a large prime modulus (for
example `M = 2^61 - 1`, using `Math.multiplyHigh` for the product) and a random base chosen at
start-up, collisions are rare, and an adversary cannot prepare inputs that collide. The expected
cost is O(n + m).

Rolling hashes shine where KMP does not:

- **Many patterns of the same length**: put their hashes in a `HashSet` and slide one window over
  the text: O(n + total pattern length) expected.
- **Longest repeated substring**: if a repeat of length L exists, one of length L - 1 does too, so
  binary search on L and check each length with a set of window hashes: O(n log n) expected.
- **Content-defined chunking**: `rsync`, backup tools and deduplicating storage cut files where a
  rolling hash of the last few bytes hits a chosen value, so an insertion only moves the cuts
  near it.

Check equal hashes against the characters whenever a wrong answer costs more than the
comparison. A collision then only costs time, never correctness.

## Other tools you should recognise

| Problem | Algorithm | Cost |
| --- | --- | --- |
| one pattern, any text | KMP, Z-function | O(n + m) |
| one pattern, natural-language text | Boyer-Moore (and Horspool): compare from the end of the pattern, skip ahead on a mismatch; GNU grep uses it | sublinear on average, O(n · m) worst for the simple variants |
| many patterns at once | Aho-Corasick: a trie of the patterns plus failure links, the KMP idea on a trie; used by `fgrep`, antivirus and intrusion detection | O(n + total pattern length + matches) |
| many queries against one fixed text | suffix array or suffix automaton, built once | O(m log n) or O(m) per query |
| approximate matches | edit distance DP (Advanced phase), bitap | O(n · m) or O(n · m / w) |

## Pitfalls

- **Overlapping matches**: `aa` occurs 3 times in `aaaa`. Restarting after the whole match finds
  only 2.
- **Unicode**: a Java `String` is UTF-16, so an emoji is two `char`s. Matching on `char` is still
  exact for equal strings, but positions are in `char` units, and case-insensitive search needs
  normalising both sides first, not lower-casing one.
- **Empty pattern**: define it explicitly (refuse it, or say it matches at every position). Many
  bugs are an infinite loop on an empty pattern.
- **Hash arithmetic**: `(h - x) % M` can be negative in Java. Add `M` before taking the remainder.

## Summary

- The naive search is O(n · m) in the worst case, and so is `indexOf` in a loop.
- KMP's prefix function makes search O(n + m), finds overlapping matches and gives the smallest
  period.
- Rolling hashes compare windows in O(1); verify equal hashes, and use them for many same-length
  patterns, repeated substrings and chunking.
- For many patterns use Aho-Corasick; for many queries against one text, a suffix array.

## Further reading

- cp-algorithms, "Prefix function. Knuth-Morris-Pratt algorithm": <https://cp-algorithms.com/string/prefix-function.html>
- cp-algorithms, "String Hashing" and "Rabin-Karp": <https://cp-algorithms.com/string/string-hashing.html>
- Sedgewick and Wayne, *Algorithms*, 4th ed., "Substring Search": <https://algs4.cs.princeton.edu/53substring/>
- Andrew Tridgell, "Efficient Algorithms for Sorting and Synchronization" (the rsync thesis, rolling checksums): <https://www.samba.org/~tridge/phd_thesis.pdf>
