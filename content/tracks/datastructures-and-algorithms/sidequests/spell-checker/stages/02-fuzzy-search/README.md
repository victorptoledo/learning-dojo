A misspelled word needs suggestions: the dictionary words within a small distance of it. Add to
`SpellChecker`:

```java
public List<String> suggest(String word, int maxDistance, int limit)
```

The dictionary words at distance `<= maxDistance` from `word` (case ignored), sorted by distance,
then by frequency (highest first), then alphabetically; at most `limit` of them. A word that is not
letters `a`-`z`, or a negative `maxDistance` or `limit`, is an `IllegalArgumentException`.

```java
// dictionary: the (500), then (40), they (30), tea (5), ten (5), to (300)
spell.suggest("thw", 1, 10);   // [the]: every other word is at distance 2
spell.suggest("thw", 2, 3);    // [the, to, then]: then the words at distance 2, by frequency
```

**Walk the trie, not the word list.** Computing `distance(word, w)` for each of 60,000 words
repeats the same work: every word under the prefix `th` shares the first two rows of its table.
Instead, walk the trie depth first and build **one row per node**: the row of a node is the row of
its parent extended by the node's letter, exactly as one more row of the table in stage 1. Where a
word ends, the last cell of the row is its distance.

**Prune.** The smallest value of a row never goes down in the rows below it (each cell of the next row is at
least the smallest cell above it). So when every value of a node's row is above `maxDistance`, no
word under that node can be close enough: skip the whole branch. With a distance of 2 most of
the trie is never visited; the tests run 500 suggestions on a dictionary of 60,000 words.
